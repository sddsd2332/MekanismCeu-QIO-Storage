package mekceuqiostorage.common.integration.embers;

import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.AbstractSingleResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import teamroots.embers.api.capabilities.EmbersCapabilities;
import teamroots.embers.api.power.IEmberCapability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Direct sided Embers capability integration, quantized to whole Ember units. */
public final class EmbersEmberTransferAdapter
      extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {

    public static final EmbersEmberTransferAdapter INSTANCE = new EmbersEmberTransferAdapter();

    private EmbersEmberTransferAdapter() {
        super(QIOStorageResourceSpecs.EMBERS_EMBER, QIOStorageResources.EMBER);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        return getCapability(target, targetFace) != null;
    }

    @Override
    protected long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        IEmberCapability capability = getCapability(target, targetFace);
        if (capability == null) {
            return 0;
        }
        try {
            Double ember = readFinite(capability.getEmber());
            return ember == null ? 0 : wholeStored(ember);
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    protected long extractResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IEmberCapability capability = getCapability(target, targetFace);
        if (capability == null) {
            return 0;
        }
        Double beforeValue;
        try {
            beforeValue = readFinite(capability.getEmber());
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        if (beforeValue == null || !QIOStorageTransferMath.isFinitePositive(beforeValue)) {
            return 0;
        }
        double before = beforeValue;
        long requested = QIOStorageTransferMath.limit(amount, wholeStored(before));
        if (requested <= 0) {
            return 0;
        }
        double reported;
        try {
            reported = capability.removeAmount(nativeAmount(requested), action.execute());
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
        IEmberCapability capability = getCapability(target, targetFace);
        if (capability == null) {
            return 0;
        }
        Double beforeValue;
        Double capacityValue;
        try {
            beforeValue = readFinite(capability.getEmber());
            capacityValue = readFinite(capability.getEmberCapacity());
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        if (beforeValue == null || capacityValue == null) {
            return 0;
        }
        double before = beforeValue;
        double capacity = capacityValue;
        if (!QIOStorageTransferMath.isFinite(before) || before < 0 ||
              !QIOStorageTransferMath.isFinitePositive(capacity)) {
            return 0;
        }
        double free = capacity - before;
        long requested = QIOStorageTransferMath.limit(amount, wholeStored(free));
        if (requested <= 0) {
            return 0;
        }
        double reported;
        try {
            reported = capability.addAmount(nativeAmount(requested), action.execute());
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
    private static IEmberCapability getCapability(TileEntity target, EnumFacing targetFace) {
        try {
            if (EmbersCapabilities.EMBER_CAPABILITY == null) {
                return null;
            }
            return target.hasCapability(EmbersCapabilities.EMBER_CAPABILITY, targetFace) ?
                  target.getCapability(EmbersCapabilities.EMBER_CAPABILITY, targetFace) : null;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Double readFinite(double value) {
        return QIOStorageTransferMath.isFinite(value) ? value : null;
    }

    private static long wholeStored(double value) {
        return QIOStorageTransferMath.isFinite(value) && value > 0 ?
              QIOStorageTransferMath.wholeUnits(value) : 0;
    }

    private static long reportedUnits(double reported, long requested) {
        return QIOStorageTransferMath.isFinite(reported) && reported >= 0 ?
              QIOStorageTransferMath.result(QIOStorageTransferMath.wholeUnits(reported), requested) : 0;
    }

    private static double nativeAmount(long amount) {
        // A double represents every integer exactly through 2^53. Capping the native request at
        // that boundary avoids rounding an otherwise valid whole-unit QIO transfer.
        return (double) Math.min(amount, 9_007_199_254_740_991L);
    }
}
