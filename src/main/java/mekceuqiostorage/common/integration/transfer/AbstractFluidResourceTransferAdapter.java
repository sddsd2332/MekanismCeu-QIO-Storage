package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

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
            long moved = action.simulate() ? drainedAmount(handler.drain(new FluidStack(fluid, requested), false), fluid) :
                  NativeTransferAccounting.reported(requested, false, () -> balance(handler, fluid),
                        () -> drainedAmount(handler.drain(new FluidStack(fluid, requested), true), fluid), null);
            if (moved > 0 && action.execute()) {
                markDirtySafely(target);
            }
            return moved;
        } catch (UncertainTransferException failure) {
            throw failure;
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
            Fluid fluid = Objects.requireNonNull(getNativeFluid(), "native fluid");
            long moved = action.simulate() ? handler.fill(new FluidStack(fluid, requested), false) :
                  NativeTransferAccounting.reported(requested, true, () -> balance(handler, fluid),
                        () -> handler.fill(new FluidStack(fluid, requested), true), null);
            if (moved > 0 && action.execute()) {
                markDirtySafely(target);
            }
            return moved;
        } catch (UncertainTransferException failure) {
            throw failure;
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

    private static long drainedAmount(FluidStack stack, Fluid fluid) {
        return isExpectedFluid(stack, fluid) ? stack.amount : 0;
    }

    private static double balance(IFluidHandler handler, Fluid fluid) {
        IFluidTankProperties[] tanks = handler.getTankProperties();
        if (tanks == null || tanks.length == 0) return Double.NaN;
        double total = 0;
        for (IFluidTankProperties tank : tanks) {
            if (tank == null) return Double.NaN;
            FluidStack contents = tank.getContents();
            if (contents != null && contents.getFluid() == fluid) {
                if (contents.amount < 0) return Double.NaN;
                total += contents.amount;
            }
        }
        return total;
    }
}
