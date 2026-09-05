package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/** Maps one native fluid to a single custom QIO identity without losing simulation semantics. */
public abstract class AbstractFluidResourceTransferAdapter<T>
      extends AbstractSingleResourceTransferAdapter<T> {

    protected AbstractFluidResourceTransferAdapter(@Nonnull QIOStorageResourceSpec<T> resourceSpec,
          @Nonnull T resource) {
        super(resourceSpec, resource);
    }

    @Nonnull
    protected abstract Fluid getNativeFluid();

    @Override
    public final boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        IFluidHandler handler = getHandler(target, targetFace);
        if (handler == null) {
            return false;
        }
        try {
            Fluid fluid = Objects.requireNonNull(getNativeFluid(), "native fluid");
            FluidStack drained = handler.drain(new FluidStack(fluid, 1), false);
            return isExpectedFluid(drained, fluid) ||
                  handler.fill(new FluidStack(fluid, 1), false) > 0;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    protected final long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        IFluidHandler handler = getHandler(target, targetFace);
        if (handler == null) {
            return 0;
        }
        try {
            Fluid fluid = Objects.requireNonNull(getNativeFluid(), "native fluid");
            FluidStack drained = handler.drain(new FluidStack(fluid, Integer.MAX_VALUE), false);
            return isExpectedFluid(drained, fluid) ?
                  QIOStorageTransferMath.limit(drained.amount, Integer.MAX_VALUE) : 0;
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    protected final long extractResource(@Nonnull TileEntity target,
          @Nonnull EnumFacing targetFace, long amount, @Nonnull Action action) {
        IFluidHandler handler = getHandler(target, targetFace);
        if (handler == null) {
            return 0;
        }
        int requested = QIOStorageTransferMath.intLimit(amount);
        if (requested <= 0) {
            return 0;
        }
        try {
            Fluid fluid = Objects.requireNonNull(getNativeFluid(), "native fluid");
            FluidStack drained = handler.drain(new FluidStack(fluid, requested), action.execute());
            long moved = isExpectedFluid(drained, fluid) ?
                  QIOStorageTransferMath.limit(drained.amount, requested) : 0;
            if (moved > 0 && action.execute()) {
                markDirtySafely(target);
            }
            return moved;
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    protected final long insertResource(@Nonnull TileEntity target,
          @Nonnull EnumFacing targetFace, long amount, @Nonnull Action action) {
        IFluidHandler handler = getHandler(target, targetFace);
        if (handler == null) {
            return 0;
        }
        int requested = QIOStorageTransferMath.intLimit(amount);
        if (requested <= 0) {
            return 0;
        }
        try {
            int moved = handler.fill(new FluidStack(
                  Objects.requireNonNull(getNativeFluid(), "native fluid"), requested), action.execute());
            moved = (int) QIOStorageTransferMath.limit(moved, requested);
            if (moved > 0 && action.execute()) {
                markDirtySafely(target);
            }
            return moved;
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    @Nullable
    private static IFluidHandler getHandler(TileEntity target, EnumFacing targetFace) {
        try {
            if (!target.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, targetFace)) {
                return null;
            }
            return target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, targetFace);
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isExpectedFluid(@Nullable FluidStack stack, Fluid fluid) {
        return stack != null && stack.amount > 0 && stack.getFluid() == fluid;
    }
}
