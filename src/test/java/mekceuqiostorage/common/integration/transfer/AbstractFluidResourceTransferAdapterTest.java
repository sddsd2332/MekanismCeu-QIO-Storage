package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractFluidResourceTransferAdapterTest {

    private static final Fluid EXPECTED = fluid("expected");
    private static final Fluid OTHER = fluid("other");

    @BeforeAll
    static void registerCodecs() {
        Bootstrap.register();
        FluidRegistry.registerFluid(EXPECTED);
        FluidRegistry.registerFluid(OTHER);
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void filtersCapabilitySidesAndUnexpectedFluids() {
        TestAdapter adapter = new TestAdapter();
        TestTileEntity target = new TestTileEntity(new TestFluidHandler(EXPECTED, 100, 250));

        assertTrue(adapter.supports(target, EnumFacing.NORTH));
        assertFalse(adapter.supports(target, EnumFacing.SOUTH));
        assertFalse(adapter.supports(new TestTileEntity(null), EnumFacing.NORTH));
        assertFalse(adapter.supports(new TestTileEntity(
              new TestFluidHandler(OTHER, 100, 250)), EnumFacing.NORTH));
    }

    @Test
    void treatsAnEmptyAcceptingTankAsSupportedButNotExtractable() {
        TestAdapter adapter = new TestAdapter();
        TestTileEntity target = new TestTileEntity(new TestFluidHandler(null, 0, 250));

        assertTrue(adapter.supports(target, EnumFacing.NORTH));
        assertTrue(adapter.getExtractable(target, EnumFacing.NORTH, 1, 100).isEmpty());
    }

    @Test
    void simulatesAndExecutesExtractionWithoutOverReporting() {
        TestAdapter adapter = new TestAdapter();
        TestFluidHandler handler = new TestFluidHandler(EXPECTED, 125, 250);
        TestTileEntity target = new TestTileEntity(handler);

        assertEquals(100, adapter.extract(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), 100, Action.SIMULATE));
        assertEquals(125, handler.amount);
        assertEquals(0, target.dirtyCalls);

        assertEquals(100, adapter.extract(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), 100, Action.EXECUTE));
        assertEquals(25, handler.amount);
        assertEquals(1, target.dirtyCalls);

        assertEquals(25, adapter.extract(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), Long.MAX_VALUE, Action.EXECUTE));
        assertEquals(0, handler.amount);
        assertEquals(2, target.dirtyCalls);
    }

    @Test
    void simulatesAndExecutesOnlyTheAmountAcceptedByTheTank() {
        TestAdapter adapter = new TestAdapter();
        TestFluidHandler handler = new TestFluidHandler(EXPECTED, 200, 250);
        TestTileEntity target = new TestTileEntity(handler);

        assertEquals(50, adapter.insert(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), 100, Action.SIMULATE));
        assertEquals(200, handler.amount);
        assertEquals(0, target.dirtyCalls);

        assertEquals(50, adapter.insert(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), 100, Action.EXECUTE));
        assertEquals(250, handler.amount);
        assertEquals(1, target.dirtyCalls);
        assertEquals(0, adapter.insert(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), 1, Action.EXECUTE));
        assertEquals(1, target.dirtyCalls);
    }

    private static Fluid fluid(String name) {
        ResourceLocation texture = new ResourceLocation("mekceuqiostorage", "blocks/" + name);
        return new Fluid("mekceuqiostorage_test_" + name, texture, texture);
    }

    private static final class TestAdapter
          extends AbstractFluidResourceTransferAdapter<QIOStorageResources.Scalar> {

        private TestAdapter() {
            super(QIOStorageResourceSpecs.BOTANIA_MANA, QIOStorageResources.MANA);
        }

        @Override
        protected Fluid getNativeFluid() {
            return EXPECTED;
        }
    }

    private static final class TestTileEntity extends TileEntity {

        @Nullable
        private final IFluidHandler handler;
        private int dirtyCalls;

        private TestTileEntity(@Nullable IFluidHandler handler) {
            this.handler = handler;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return handler != null && facing == EnumFacing.NORTH &&
                  capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return hasCapability(capability, facing) ? (T) handler : null;
        }

        @Override
        public void markDirty() {
            dirtyCalls++;
        }
    }

    private static final class TestFluidHandler implements IFluidHandler {

        @Nullable
        private Fluid fluid;
        private int amount;
        private final int capacity;

        private TestFluidHandler(@Nullable Fluid fluid, int amount, int capacity) {
            this.fluid = fluid;
            this.amount = amount;
            this.capacity = capacity;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            FluidStack contents = fluid == null || amount <= 0 ? null : new FluidStack(fluid, amount);
            return new IFluidTankProperties[]{
                  new FluidTankProperties(contents, capacity, true, true)
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0 ||
                  fluid != null && fluid != resource.getFluid()) {
                return 0;
            }
            int accepted = Math.min(resource.amount, capacity - amount);
            if (accepted > 0 && doFill) {
                fluid = resource.getFluid();
                amount += accepted;
            }
            return accepted;
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0 || fluid != resource.getFluid()) {
                return null;
            }
            return drain(resource.amount, doDrain);
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0 || fluid == null || amount <= 0) {
                return null;
            }
            int drained = Math.min(maxDrain, amount);
            FluidStack result = new FluidStack(fluid, drained);
            if (doDrain) {
                amount -= drained;
                if (amount == 0) {
                    fluid = null;
                }
            }
            return result;
        }
    }
}
