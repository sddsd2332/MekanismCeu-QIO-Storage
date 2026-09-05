package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.soul.EnumDemonWillType;
import WayofTime.bloodmagic.soul.IDemonWillGem;
import WayofTime.bloodmagic.tile.TileDemonCrucible;
import WayofTime.bloodmagic.tile.TileDemonCrystallizer;
import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMagicWillTransferAdapterTest {

    private static final BloodMagicWillTransferAdapter ADAPTER = BloodMagicWillTransferAdapter.INSTANCE;

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void integerConversionKeepsNativeRemaindersAndRejectsInvalidAmounts() {
        assertEquals(0, BloodMagicWillMath.toQIOUnits(0.29));
        assertEquals(1, BloodMagicWillMath.toQIOUnits(1.239));
        assertEquals(0, BloodMagicWillMath.toQIOUnits(0.009));
        assertEquals(0, BloodMagicWillMath.toQIOUnits(Double.NaN));
        assertEquals(0, BloodMagicWillMath.toQIOUnits(Double.POSITIVE_INFINITY));
        assertEquals(0, BloodMagicWillMath.toQIOUnits(-1));
        assertEquals(Long.MAX_VALUE, BloodMagicWillMath.toQIOUnits(Double.MAX_VALUE));
        assertEquals(29, BloodMagicWillMath.toWill(29));
    }

    @Test
    void allTypesTransferThroughTheNativeCrucibleWithoutMixingOrDroppingRemainders() {
        TestCrucible crucible = attach(new TestCrucible());
        for (DemonWill type : DemonWill.values()) {
            EnumDemonWillType nativeType = EnumDemonWillType.valueOf(type.name());
            crucible.willMap.put(nativeType, 1.295);
            assertEquals(1, ADAPTER.extract(crucible, EnumFacing.DOWN,
                  QIOStorageDescriptors.will(type), 100, Action.SIMULATE));
            assertEquals(1, ADAPTER.extract(crucible, EnumFacing.DOWN,
                  QIOStorageDescriptors.will(type), 100, Action.EXECUTE));
            assertEquals(0.295, crucible.getCurrentWill(nativeType), 1E-12);
            assertEquals(0, ADAPTER.extract(crucible, EnumFacing.DOWN,
                  QIOStorageDescriptors.will(type), 1, Action.EXECUTE));
            assertEquals(1, ADAPTER.insert(crucible, EnumFacing.UP,
                  QIOStorageDescriptors.will(type), 1, Action.SIMULATE));
            assertEquals(0.295, crucible.getCurrentWill(nativeType), 1E-12);
            assertEquals(1, ADAPTER.insert(crucible, EnumFacing.UP,
                  QIOStorageDescriptors.will(type), 1, Action.EXECUTE));
            assertEquals(1.295, crucible.getCurrentWill(nativeType), 1E-12);
        }
        assertEquals(5, crucible.willMap.size());
    }

    @Test
    void crucibleSoulGemIsExtractedThroughItsNativeGemApi() {
        TestCrucible crucible = attach(new TestCrucible());
        TestWillGem gem = new TestWillGem();
        ItemStack stack = new ItemStack(gem);
        gem.setWill(EnumDemonWillType.DESTRUCTIVE, stack, 3.25);
        crucible.putGem(stack);
        crucible.willMap.put(EnumDemonWillType.DESTRUCTIVE, 9D);

        assertEquals(3, ADAPTER.extract(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), 100, Action.SIMULATE));
        assertEquals(3, ADAPTER.extract(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), 100, Action.EXECUTE));
        assertEquals(0.25, gem.getWill(EnumDemonWillType.DESTRUCTIVE, stack), 1E-12);
        assertEquals(9D, crucible.getCurrentWill(EnumDemonWillType.DESTRUCTIVE), 1E-12);
    }

    @Test
    void populatedSoulGemNbtIsExtractedWithoutCrucibleWillMapEntry() {
        TestCrucible crucible = attach(new TestCrucible());
        TestWillGem gem = new TestWillGem();
        ItemStack stack = new ItemStack(gem);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("demonWillType", "destructive");
        tag.setDouble("souls", 5.25D);
        stack.setTagCompound(tag);
        crucible.putGem(stack);

        assertTrue(crucible.willMap.isEmpty());
        assertEquals(5.25D, ((IDemonWillGem) stack.getItem()).getWill(
              EnumDemonWillType.DESTRUCTIVE, stack), 1E-12);
        assertEquals(5, ADAPTER.extract(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), 100, Action.SIMULATE));
        assertEquals(5, ADAPTER.extract(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), 100, Action.EXECUTE));
        assertEquals(0.25D, ((IDemonWillGem) stack.getItem()).getWill(
              EnumDemonWillType.DESTRUCTIVE, stack), 1E-12);
        assertTrue(crucible.willMap.isEmpty());
    }

    @Test
    void directExecuteCapsTheNativeHolderWhichOtherwiseAddsTheUnboundedRequest() {
        TileDemonCrystallizer crystallizer = attach(new TileDemonCrystallizer());
        crystallizer.holder.addWill(EnumDemonWillType.CORROSIVE, 99);
        assertEquals(1, ADAPTER.insert(crystallizer, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.CORROSIVE), Long.MAX_VALUE, Action.EXECUTE));
        assertEquals(100, crystallizer.getCurrentWill(EnumDemonWillType.CORROSIVE));
        assertEquals(0, ADAPTER.insert(crystallizer, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.CORROSIVE), 100, Action.EXECUTE));
        assertEquals(0, crystallizer.getCurrentWill(EnumDemonWillType.DEFAULT));
    }

    @Test
    void executeRechecksCapacityAfterEarlierSimulation() {
        TestCrucible crucible = attach(new TestCrucible());
        assertEquals(100, ADAPTER.insert(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DEFAULT), 100, Action.SIMULATE));
        crucible.willMap.put(EnumDemonWillType.DEFAULT, 99D);
        assertEquals(1, ADAPTER.insert(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DEFAULT), 100, Action.EXECUTE));
        assertEquals(100, crucible.getCurrentWill(EnumDemonWillType.DEFAULT));
    }

    @Test
    void rotatingDiscoveryAllowsEveryTypeWithinTheImporterBudget() {
        TestCrucible crucible = attach(new TestCrucible());
        for (EnumDemonWillType type : EnumDemonWillType.values()) {
            crucible.willMap.put(type, 100D);
        }
        Set<DemonWill> discovered = new HashSet<>();
        for (int tick = 0; tick < 5; tick++) {
            crucible.getWorld().setTotalWorldTime(tick * 11L);
            List<QIOResourceStack> candidates = ADAPTER.getExtractable(crucible, EnumFacing.NORTH, 4, 64);
            assertEquals(1, candidates.size());
            assertEquals(64, candidates.get(0).getAmount());
            discovered.add(candidates.get(0).getDescriptor().resolve(QIOStorageCodecs.BLOODMAGIC_WILL));
        }
        assertEquals(5, discovered.size());
        for (EnumDemonWillType type : EnumDemonWillType.values()) {
            assertEquals(100, crucible.getCurrentWill(type));
        }
    }

    @Test
    void directionFlagsAndInvalidContextsPreventTransfers() {
        TestCrucible crucible = attach(new TestCrucible());
        crucible.willMap.put(EnumDemonWillType.DEFAULT, 1D);
        crucible.drain = false;
        assertTrue(ADAPTER.getExtractable(crucible, EnumFacing.NORTH, 4, 64).isEmpty());
        crucible.fill = false;
        assertEquals(0, ADAPTER.insert(crucible, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DEFAULT), 1, Action.EXECUTE));
        assertEquals(0, ADAPTER.insert(crucible, EnumFacing.NORTH, QIOStorageDescriptors.mana(), 1, Action.EXECUTE));
        ((TestWorld) crucible.getWorld()).loaded = false;
        assertFalse(ADAPTER.supports(crucible, EnumFacing.NORTH));
        new TestWorld(true).attach(crucible);
        assertFalse(ADAPTER.supports(crucible, EnumFacing.NORTH));
        assertFalse(ADAPTER.supports(new TestCrucible(), EnumFacing.NORTH));
        attach(crucible).invalidate();
        assertFalse(ADAPTER.supports(crucible, EnumFacing.NORTH));
    }

    private static <T extends TileEntity> T attach(T tile) {
        new TestWorld(false).attach(tile);
        return tile;
    }

    private static final class TestCrucible extends TileDemonCrucible {
        private boolean fill = true;
        private boolean drain = true;

        @Override
        public boolean canFill(EnumDemonWillType type) {
            return fill;
        }

        @Override
        public boolean canDrain(EnumDemonWillType type) {
            return drain;
        }

        @Override
        public void markDirty() {
        }

        private void putGem(ItemStack stack) {
            inventory.set(0, stack);
        }
    }

    private static final class TestWillGem extends Item implements IDemonWillGem {

        private final Map<EnumDemonWillType, Double> wills =
              new EnumMap<>(EnumDemonWillType.class);

        @Override
        public ItemStack fillDemonWillGem(ItemStack gem, ItemStack will) {
            return gem;
        }

        @Override
        public double getWill(EnumDemonWillType type, ItemStack stack) {
            if (stack.hasTagCompound() && type.toString().equals(stack.getTagCompound().getString("demonWillType"))) {
                return stack.getTagCompound().getDouble("souls");
            }
            Double value = wills.get(type);
            return value == null ? 0 : value;
        }

        @Override
        public void setWill(EnumDemonWillType type, ItemStack stack, double amount) {
            if (stack.hasTagCompound() && type.toString().equals(stack.getTagCompound().getString("demonWillType"))) {
                stack.getTagCompound().setDouble("souls", amount);
            }
            wills.put(type, amount);
        }

        @Override
        public int getMaxWill(EnumDemonWillType type, ItemStack stack) {
            return 16_384;
        }

        @Override
        public double drainWill(EnumDemonWillType type, ItemStack stack, double amount,
              boolean execute) {
            double current = getWill(type, stack);
            double moved = Math.min(current, amount);
            if (execute) {
                setWill(type, stack, current - moved);
            }
            return moved;
        }

        @Override
        public double fillWill(EnumDemonWillType type, ItemStack stack, double amount,
              boolean execute) {
            double current = getWill(type, stack);
            double moved = Math.min(getMaxWill(type, stack) - current, amount);
            if (execute) {
                setWill(type, stack, current + moved);
            }
            return moved;
        }
    }

    private static final class TestWorld extends World {
        private TileEntity tile;
        private boolean loaded = true;

        private TestWorld(boolean remote) {
            super(null, new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), remote);
        }

        private void attach(TileEntity tile) {
            this.tile = tile;
            tile.setWorld(this);
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return tile != null && tile.getPos().equals(pos) ? tile : null;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return loaded;
        }
    }
}
