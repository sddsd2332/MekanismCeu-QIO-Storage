package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Complete QIO-side behavior for a resource with one canonical identity. Native integrations
 * provide capability lookup plus three bounded operations and retain their API-specific units.
 */
public abstract class AbstractSingleResourceTransferAdapter<T>
        extends AbstractQIOResourceTransferAdapter<T> {

    private final T resource;

    protected AbstractSingleResourceTransferAdapter(@Nonnull QIOStorageResourceSpec<T> resourceSpec,
                                                    @Nonnull T resource) {
        super(resourceSpec);
        this.resource = getCodec().normalize(Objects.requireNonNull(resource, "resource template"));
    }

    @Override
    @Nonnull
    public final List<QIOResourceStack> getExtractable(@Nonnull TileEntity target,
                                                       @Nonnull EnumFacing targetFace, int maximumTypes, long maximumAmount) {
        if (maximumTypes <= 0 || maximumAmount <= 0 || TransferRecovery.isBlocked(target)) {
            return Collections.emptyList();
        }
        try {
            return supports(target, targetFace) ?
                  candidate(resource, getStored(target, targetFace), maximumTypes, maximumAmount) :
                  Collections.emptyList();
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    protected final long extractResolved(@Nonnull TileEntity target,
                                         @Nonnull EnumFacing targetFace, @Nonnull T resolvedResource, long amount,
                                         @Nonnull Action action) {
        return extractResource(target, targetFace, amount, action);
    }

    @Override
    protected final long insertResolved(@Nonnull TileEntity target,
                                        @Nonnull EnumFacing targetFace, @Nonnull T resolvedResource, long amount,
                                        @Nonnull Action action) {
        return insertResource(target, targetFace, amount, action);
    }

    /**
     * Current whole native units. Negative or unavailable values are treated as empty.
     */
    protected abstract long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace);

    /**
     * Returns the number of native units actually extracted or simulatable.
     */
    protected abstract long extractResource(@Nonnull TileEntity target,
                                            @Nonnull EnumFacing targetFace, long amount, @Nonnull Action action);

    /**
     * Returns the number of native units actually inserted or simulatable.
     */
    protected abstract long insertResource(@Nonnull TileEntity target,
                                           @Nonnull EnumFacing targetFace, long amount, @Nonnull Action action);
}
