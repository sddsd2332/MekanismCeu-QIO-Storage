package mekceuqiostorage.common.integration.pneumaticcraft;

import mekanism.api.Action;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IPneumaticMachine;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.AbstractSingleResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Stores PneumaticCraft's integer air amount; pressure remains air divided by handler volume. */
public final class PneumaticCraftAirTransferAdapter
      extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {

    public static final PneumaticCraftAirTransferAdapter INSTANCE =
          new PneumaticCraftAirTransferAdapter();

    private PneumaticCraftAirTransferAdapter() {
        super(QIOStorageResourceSpecs.PNEUMATICCRAFT_AIR, QIOStorageResources.AIR);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        return getHandler(target, targetFace) != null;
    }

    @Override
    protected long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        IAirHandler handler = getHandler(target, targetFace);
        Integer air = handler == null ? null : readAir(handler);
        return air == null ? 0 : air;
    }

    @Override
    protected long extractResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IAirHandler handler = getHandler(target, targetFace);
        if (handler == null) {
            return 0;
        }
        Integer beforeValue = readAir(handler);
        if (beforeValue == null) {
            return 0;
        }
        int before = beforeValue;
        int requested = QIOStorageTransferMath.intLimit(
              QIOStorageTransferMath.limit(amount, before));
        if (requested <= 0) {
            return requested;
        }
        if (action.simulate()) {
            return requested;
        }
        try {
            handler.addAir(-requested);
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        Integer afterValue = readAir(handler);
        if (afterValue == null) {
            compensate(handler, requested);
            return 0;
        }
        int after = afterValue;
        long movedLong = QIOStorageTransferMath.decrease(before, after);
        if (after > before) {
            compensate(handler, -(after - before));
            return 0;
        }
        if (movedLong > requested) {
            compensate(handler, (int) Math.min(Integer.MAX_VALUE, movedLong - requested));
            movedLong = requested;
        }
        int moved = (int) movedLong;
        if (moved > 0) {
            markDirtySafely(target);
        }
        return moved;
    }

    @Override
    protected long insertResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IAirHandler handler = getHandler(target, targetFace);
        if (handler == null) {
            return 0;
        }
        Integer beforeValue = readAir(handler);
        if (beforeValue == null) {
            return 0;
        }
        int before = beforeValue;
        long capacity = safeAirCapacity(handler);
        if (capacity <= before) {
            return 0;
        }
        int requested = QIOStorageTransferMath.intLimit(
              QIOStorageTransferMath.limit(amount, capacity - before));
        if (requested <= 0) {
            return requested;
        }
        if (action.simulate()) {
            return requested;
        }
        try {
            handler.addAir(requested);
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        Integer afterValue = readAir(handler);
        if (afterValue == null) {
            compensate(handler, -requested);
            return 0;
        }
        int after = afterValue;
        long movedLong = QIOStorageTransferMath.increase(before, after);
        if (after < before) {
            compensate(handler, requested);
            return 0;
        }
        if (movedLong > requested) {
            compensate(handler, (int) -Math.min(Integer.MAX_VALUE, movedLong - requested));
            movedLong = requested;
        }
        int moved = (int) movedLong;
        if (moved > 0) {
            markDirtySafely(target);
        }
        return moved;
    }

    private static long safeAirCapacity(IAirHandler handler) {
        // Danger pressure is only an alarm threshold. The handler's max pressure is the
        // actual storage limit, and air is pressure multiplied by the handler volume.
        double pressure;
        int volume;
        try {
            pressure = handler.getMaxPressure();
            volume = handler.getVolume();
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        if (!QIOStorageTransferMath.isFinitePositive(pressure) || volume <= 0) {
            return 0;
        }
        double capacity = pressure * (double) volume;
        if (!QIOStorageTransferMath.isFinitePositive(capacity)) {
            return 0;
        }
        if (capacity >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        long whole = (long) Math.floor(capacity);
        return Math.max(0L, whole);
    }

    @Nullable
    private static IAirHandler getHandler(TileEntity target, EnumFacing targetFace) {
        try {
            IPneumaticMachine machine = IPneumaticMachine.getMachine(target);
            return machine == null ? null : machine.getAirHandler(targetFace);
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer readAir(IAirHandler handler) {
        try {
            int air = handler.getAir();
            // PneumaticCraft uses negative air for vacuum/negative pressure. QIO stores only
            // actual non-negative air and must not silently reinterpret a vacuum as an empty tank.
            return air < 0 ? null : air;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static void compensate(IAirHandler handler, int amount) {
        if (amount == 0) {
            return;
        }
        try {
            handler.addAir(amount);
        } catch (LinkageError | RuntimeException ignored) {
            // Best-effort recovery when a provider reports an inconsistent post-state.
        }
    }
}
