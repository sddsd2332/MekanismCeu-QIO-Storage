package mekceuqiostorage.common.integration.naturesaura;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.container.IAuraContainer;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.AbstractSingleResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Direct sided Nature's Aura container capability integration. */
public final class NaturesAuraTransferAdapter
      extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {

    public static final NaturesAuraTransferAdapter INSTANCE = new NaturesAuraTransferAdapter();

    private NaturesAuraTransferAdapter() {
        super(QIOStorageResourceSpecs.NATURESAURA_AURA, QIOStorageResources.AURA);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        return getContainer(target, targetFace) != null;
    }

    @Override
    protected long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        IAuraContainer container = getContainer(target, targetFace);
        Integer stored = container == null ? null : readStoredAura(container);
        return stored == null ? 0 : stored;
    }

    @Override
    protected long extractResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IAuraContainer container = getContainer(target, targetFace);
        if (container == null) {
            return 0;
        }
        Integer beforeValue = readStoredAura(container);
        if (beforeValue == null) {
            return 0;
        }
        int before = beforeValue;
        int requested = QIOStorageTransferMath.intLimit(
              QIOStorageTransferMath.limit(amount, before));
        if (requested <= 0) {
            return 0;
        }
        int reported;
        try {
            reported = container.drainAura(requested, action.simulate());
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        long moved = reportedUnits(reported, requested);
        if (moved > 0 && action.execute()) {
            markDirtySafely(target);
        }
        return moved;
    }

    @Override
    protected long insertResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IAuraContainer container = getContainer(target, targetFace);
        if (container == null || target.getWorld() == null) {
            return 0;
        }
        IAuraType worldType;
        try {
            worldType = IAuraType.forWorld(target.getWorld());
            if (worldType == null || !container.isAcceptableType(worldType)) {
                return 0;
            }
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        Integer beforeValue = readStoredAura(container);
        Integer maximumValue = readMaxAura(container);
        if (beforeValue == null || maximumValue == null) {
            return 0;
        }
        int before = beforeValue;
        int maximum = maximumValue;
        if (before < 0 || maximum <= 0 || before > maximum) {
            return 0;
        }
        long free = maximum - (long) before;
        int requested = QIOStorageTransferMath.intLimit(
              QIOStorageTransferMath.limit(amount, free));
        if (requested <= 0) {
            return 0;
        }
        int reported;
        try {
            reported = container.storeAura(requested, action.simulate());
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        long moved = reportedUnits(reported, requested);
        if (moved > 0 && action.execute()) {
            markDirtySafely(target);
        }
        return moved;
    }

    @Nullable
    private static IAuraContainer getContainer(TileEntity target, EnumFacing targetFace) {
        try {
            if (NaturesAuraAPI.capAuraContainer == null) {
                return null;
            }
            return target.hasCapability(NaturesAuraAPI.capAuraContainer, targetFace) ?
                  target.getCapability(NaturesAuraAPI.capAuraContainer, targetFace) : null;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer readStoredAura(IAuraContainer container) {
        try {
            int stored = container.getStoredAura();
            return stored < 0 ? null : stored;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer readMaxAura(IAuraContainer container) {
        try {
            int maximum = container.getMaxAura();
            return maximum < 0 ? null : maximum;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static long reportedUnits(int reported, long requested) {
        return QIOStorageTransferMath.result(reported, requested);
    }
}
