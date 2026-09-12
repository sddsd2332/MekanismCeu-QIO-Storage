package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceCodec;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.api.qio.resource.QIOResourceStack;
import mekanism.api.qio.resource.QIOResourceTransferAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Safe baseline for an integration adapter. Subclasses only need to implement capability lookup
 * and the actual native API operations; all descriptor ownership and request bounds are shared.
 */
public abstract class AbstractQIOResourceTransferAdapter<T> implements QIOResourceTransferAdapter {

    private final QIOStorageResourceSpec<T> resourceSpec;

    protected AbstractQIOResourceTransferAdapter(@Nonnull QIOStorageResourceSpec<T> resourceSpec) {
        this.resourceSpec = Objects.requireNonNull(resourceSpec, "QIO resource specification");
    }

    @Nonnull
    public final QIOStorageResourceSpec<T> getResourceSpec() {
        return resourceSpec;
    }

    @Nonnull
    protected final QIOResourceCodec<T> getCodec() {
        return resourceSpec.getCodec();
    }

    @Override
    @Nonnull
    public final ResourceLocation getCodecId() {
        return resourceSpec.getCodecId();
    }

    @Override
    public final long extract(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
                              @Nonnull QIOResourceDescriptor descriptor, long amount, @Nonnull Action action) {
        Objects.requireNonNull(action, "transfer action");
        long requested = QIOStorageTransferMath.limit(amount, Long.MAX_VALUE);
        T resource = resolve(descriptor);
        if (requested <= 0 || resource == null || TransferRecovery.isBlocked(target) || !safeSupports(target, targetFace)) {
            return 0;
        }
        try {
            return QIOStorageTransferMath.result(
                  extractResolved(target, targetFace, resource, requested, action), requested);
        } catch (UncertainTransferException failure) {
            return action.simulate() ? 0 : TransferRecovery.hold(target, targetFace, descriptor, requested, false, failure);
        } catch (LinkageError | RuntimeException failure) {
            return action.simulate() ? 0 : TransferRecovery.hold(target, targetFace, descriptor, requested, false,
                  new UncertainTransferException(requested, null, null, null, failure));
        }
    }

    @Override
    public final long insert(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
                             @Nonnull QIOResourceDescriptor descriptor, long amount, @Nonnull Action action) {
        Objects.requireNonNull(action, "transfer action");
        long requested = QIOStorageTransferMath.limit(amount, Long.MAX_VALUE);
        T resource = resolve(descriptor);
        if (requested <= 0 || resource == null || TransferRecovery.isBlocked(target) || !safeSupports(target, targetFace)) {
            return 0;
        }
        try {
            return QIOStorageTransferMath.result(
                  insertResolved(target, targetFace, resource, requested, action), requested);
        } catch (UncertainTransferException failure) {
            return action.simulate() ? 0 : TransferRecovery.hold(target, targetFace, descriptor, requested, true, failure);
        } catch (LinkageError | RuntimeException failure) {
            return action.simulate() ? 0 : TransferRecovery.hold(target, targetFace, descriptor, requested, true,
                  new UncertainTransferException(requested, null, null, null, failure));
        }
    }

    /**
     * Resolves only descriptors owned by this adapter and by the currently registered codec.
     */
    @Nullable
    protected final T resolve(@Nullable QIOResourceDescriptor descriptor) {
        try {
            if (descriptor == null || !getCodecId().equals(descriptor.getCodecId()) ||
                  !descriptor.isResolved()) {
                return null;
            }
            return descriptor.resolve(getCodec());
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nonnull
    protected final QIOResourceDescriptor descriptor(@Nonnull T value) {
        return QIOResourceDescriptor.of(getCodec(), value);
    }

    /**
     * Dirty notifications are bookkeeping only. A provider must not turn a successful native
     * transfer into a zero-sized QIO transfer just because its tile rejects the notification.
     */
    protected final void markDirtySafely(@Nonnull TileEntity target) {
        try {
            target.markDirty();
        } catch (LinkageError | RuntimeException ignored) {
            // The resource operation has already completed; keep its reported amount authoritative.
        }
    }

    /**
     * Creates one bounded candidate, or an empty list when the request cannot carry a value.
     */
    @Nonnull
    protected final List<QIOResourceStack> candidate(@Nonnull T value, long amount,
                                                     int maximumTypes, long maximumAmount) {
        if (maximumTypes <= 0) {
            return Collections.emptyList();
        }
        long bounded = QIOStorageTransferMath.limit(amount, maximumAmount);
        if (bounded <= 0) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new QIOResourceStack(descriptor(value), bounded));
    }

    /**
     * Performs extraction after common descriptor, side, request, and action validation.
     */
    protected abstract long extractResolved(@Nonnull TileEntity target,
                                            @Nonnull EnumFacing targetFace, @Nonnull T resource, long amount,
                                            @Nonnull Action action);

    /**
     * Performs insertion after common descriptor, side, request, and action validation.
     */
    protected abstract long insertResolved(@Nonnull TileEntity target,
                                           @Nonnull EnumFacing targetFace, @Nonnull T resource, long amount,
                                           @Nonnull Action action);

    private boolean safeSupports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        try {
            return supports(target, targetFace);
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
