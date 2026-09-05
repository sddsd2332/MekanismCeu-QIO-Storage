package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.altar.AltarTier;
import WayofTime.bloodmagic.block.BlockLifeEssence;
import WayofTime.bloodmagic.core.data.Binding;
import WayofTime.bloodmagic.iface.IBindable;
import WayofTime.bloodmagic.orb.BloodOrb;
import WayofTime.bloodmagic.orb.IBloodOrb;
import WayofTime.bloodmagic.tile.TileAltar;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fluids.FluidRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMagicSoulNetworkEndpointTest {

    private static final UUID OWNER = UUID.fromString(
          "10000000-0000-0000-0000-000000000001");
    private static final BloodOrb ORB = new BloodOrb("test", 2, 1_000, 20);

    @BeforeAll
    static void bootstrapRegistries() {
        Bootstrap.register();
        if (!FluidRegistry.isFluidRegistered(BlockLifeEssence.getLifeEssence().getName())) {
            FluidRegistry.registerFluid(BlockLifeEssence.getLifeEssence());
        }
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void serverAltarWithBoundTierCompatibleOrbSelectsItsOwner() {
        TestAltar altar = new TestAltar(new TestWorld(false), boundOrb(), AltarTier.TWO,
              1.5F, 0.5F);

        BloodMagicSoulNetworkEndpoint endpoint = BloodMagicSoulNetworkEndpoint.resolve(altar);
        assertNotNull(endpoint);
        assertEquals(OWNER, endpoint.getResource().getOwnerId());
        assertEquals("Alice", endpoint.getResource().getOwnerName());
        assertEquals(1_500, endpoint.getMaximum());
        assertEquals(300, endpoint.getTransferLimit());
        assertEquals(25, endpoint.ticket(25).getAmount());
        assertTrue(BloodMagicLPTransferAdapter.INSTANCE.supports(altar, EnumFacing.NORTH));
        assertTrue(BloodMagicLPTransferAdapter.INSTANCE.supports(altar, EnumFacing.DOWN));

        QIOResourceDescriptor sameOwner = QIOResourceDescriptor.of(
              QIOStorageCodecs.BLOODMAGIC_LP, SoulNetworkLP.of(OWNER, "RenamedAlice"));
        assertTrue(endpoint.owns(sameOwner.resolve(QIOStorageCodecs.BLOODMAGIC_LP)));
    }

    @Test
    void invalidAltarContextsAreRejected() {
        TestWorld server = new TestWorld(false);
        assertNull(BloodMagicSoulNetworkEndpoint.resolve(new TestAltar(server,
              ItemStack.EMPTY, AltarTier.TWO, 1F, 0F)));
        assertNull(BloodMagicSoulNetworkEndpoint.resolve(new TestAltar(server,
              new ItemStack(new TestBloodOrbItem()), AltarTier.TWO, 1F, 0F)));
        assertNull(BloodMagicSoulNetworkEndpoint.resolve(new TestAltar(server,
              boundOrb(), AltarTier.ONE, 1F, 0F)));
        TestAltar clientAltar = new TestAltar(new TestWorld(true), boundOrb(), AltarTier.TWO,
              1F, 0F);
        assertNull(BloodMagicSoulNetworkEndpoint.resolve(clientAltar));
        assertFalse(BloodMagicLPTransferAdapter.INSTANCE.supports(clientAltar,
              EnumFacing.NORTH));
    }

    private static ItemStack boundOrb() {
        ItemStack stack = new ItemStack(new TestBloodOrbItem());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("binding", new Binding(OWNER, "Alice").serializeNBT());
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class TestBloodOrbItem extends Item implements IBloodOrb, IBindable {

        @Override
        public BloodOrb getOrb(ItemStack stack) {
            return ORB;
        }
    }

    private static final class TestAltar extends TileAltar {

        private final ItemStack orb;
        private final AltarTier tier;
        private final float orbMultiplier;
        private final float speedBonus;

        private TestAltar(World world, ItemStack orb, AltarTier tier, float orbMultiplier,
              float speedBonus) {
            this.orb = orb;
            this.tier = tier;
            this.orbMultiplier = orbMultiplier;
            this.speedBonus = speedBonus;
            setWorld(world);
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index == 0 ? orb : ItemStack.EMPTY;
        }

        @Override
        public AltarTier getTier() {
            return tier;
        }

        @Override
        public float getOrbMultiplier() {
            return orbMultiplier;
        }

        @Override
        public float getConsumptionMultiplier() {
            return speedBonus;
        }
    }

    private static final class TestWorld extends World {

        private TestWorld(boolean remote) {
            super(null, new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(),
                  new Profiler(), remote);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }
    }
}
