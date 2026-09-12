package mekceuqiostorage.common.integration.astralsorcery;

import hellfirepvp.astralsorcery.common.constellation.ConstellationBase;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.item.crystal.ItemTunedRockCrystal;
import hellfirepvp.astralsorcery.common.starlight.IStarlightReceiver;
import hellfirepvp.astralsorcery.common.starlight.transmission.IPrismTransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.ITransmissionReceiver;
import hellfirepvp.astralsorcery.common.starlight.transmission.base.SimpleTransmissionReceiver;
import hellfirepvp.astralsorcery.common.starlight.transmission.registry.TransmissionClassRegistry;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import hellfirepvp.astralsorcery.common.tile.TileRitualPedestal;
import hellfirepvp.astralsorcery.common.tile.TileStarlightInfuser;
import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstralSorceryStarlightTransferAdapterTest {

    private static final AstralSorceryStarlightTransferAdapter ADAPTER =
          AstralSorceryStarlightTransferAdapter.INSTANCE;

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void simulationDoesNotCreateNodesOrCallReceivers() {
        TestReceiver target = attach(new TestReceiver());
        for (EnumFacing side : EnumFacing.values()) {
            assertTrue(ADAPTER.supports(target, side));
            assertEquals(200, ADAPTER.insert(target, side, QIOStorageDescriptors.starlight(),
                  200, Action.SIMULATE));
        }
        assertEquals(0, target.nodeLookups);
        assertEquals(0, target.createdEndpoints);
        assertEquals(0, target.fallback.calls);
    }

    @Test
    void unknownReceiverUsesItsFactoryAndPaysForTheWholeEmission() {
        TestReceiver target = attach(new TestReceiver());
        assertEquals(250, insert(target, 250, Action.EXECUTE));
        assertEquals(1, target.createdEndpoints);
        assertEquals(1, target.fallback.calls);
        assertEquals(1.25, target.fallback.amount);
        assertTrue(target.fallback.chunkLoaded);
        assertSame(target.getWorld(), target.fallback.world);
        assertNull(target.fallback.constellation);
    }

    @Test
    void existingNodeRetainsTheReceiverState() {
        TestReceiver target = attach(new TestReceiver());
        target.node = new RecordingEndpoint(target.getPos());
        assertEquals(200, insert(target, 200, Action.EXECUTE));
        assertEquals(1, target.node.calls);
        assertEquals(0, target.createdEndpoints);
        assertEquals(0, target.fallback.calls);
    }

    @Test
    void realAltarCallbackFillsAndContinuesEmittingAtCapacity() {
        TestAltar altar = attach(new TestAltar(TileAltar.AltarLevel.DISCOVERY));
        assertEquals(200, insert(altar, 200, Action.EXECUTE));
        assertEquals(200, altar.getStarlightStored());
        assertEquals(10_000, insert(altar, 10_000, Action.EXECUTE));
        assertEquals(altar.getMaxStarlightStorage(), altar.getStarlightStored());
        assertEquals(200, insert(altar, 200, Action.SIMULATE));
        assertEquals(200, insert(altar, 200, Action.EXECUTE));
        assertEquals(altar.getMaxStarlightStorage(), altar.getStarlightStored());
        assertFalse(altar.getMultiblockState());
    }

    @Test
    void emptyInfuserCallbackStillCountsAsEmission() {
        TileStarlightInfuser infuser = attach(new TileStarlightInfuser() {
            @Override
            public IPrismTransmissionNode getNode() {
                return null;
            }

            @Override
            public void markDirty() {
            }
        });
        assertTrue(ADAPTER.supports(infuser, EnumFacing.NORTH));
        assertEquals(200, insert(infuser, 200, Action.EXECUTE));
    }

    @Test
    void traitAltarSelectsItsCrystalAndUpdatesAfterTheCrystalChanges() {
        TestAltar altar = attach(new TestAltar(TileAltar.AltarLevel.TRAIT_CRAFT));
        altar.endpoint = new RecordingEndpoint(altar.getPos());
        IWeakConstellation first = new ConstellationBase.Weak("qio_first");
        IWeakConstellation second = new ConstellationBase.Weak("qio_second");
        altar.setFocusStack(new ItemStack(new TestCrystal(first)));
        assertEquals(200, insert(altar, 200, Action.EXECUTE));
        assertSame(first, altar.endpoint.constellation);
        altar.setFocusStack(new ItemStack(new TestCrystal(second)));
        assertEquals(200, insert(altar, 200, Action.EXECUTE));
        assertSame(second, altar.endpoint.constellation);
        assertEquals(1, altar.getFocusItem().getCount());
        altar.setFocusStack(ItemStack.EMPTY);
        assertEquals(200, insert(altar, 200, Action.EXECUTE));
        assertNull(altar.endpoint.constellation);
    }

    @Test
    void altarWithoutFocusSlotIgnoresEvenAStoredCrystal() {
        for (TileAltar.AltarLevel level : new TileAltar.AltarLevel[]{
              TileAltar.AltarLevel.DISCOVERY, TileAltar.AltarLevel.ATTUNEMENT,
              TileAltar.AltarLevel.CONSTELLATION_CRAFT}) {
            TestAltar altar = attach(new TestAltar(level));
            altar.endpoint = new RecordingEndpoint(altar.getPos());
            altar.setFocusStack(new ItemStack(new TestCrystal(new ConstellationBase.Weak("qio_hidden"))));
            assertEquals(200, insert(altar, 200, Action.EXECUTE));
            assertNull(altar.endpoint.constellation);
        }
    }

    @Test
    void missingInvalidOrMalformedCrystalSelectsGenericLight() {
        TestAltar altar = attach(new TestAltar(TileAltar.AltarLevel.TRAIT_CRAFT));
        altar.endpoint = new RecordingEndpoint(altar.getPos());
        for (ItemStack focus : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STICK),
              new ItemStack(new TestCrystal(null)), new ItemStack(new ItemTunedRockCrystal() {
                  @Override
                  public IConstellation getFocusConstellation(ItemStack stack) {
                      throw new IllegalArgumentException("malformed crystal");
                  }
              })}) {
            altar.setFocusStack(focus);
            assertEquals(200, insert(altar, 200, Action.EXECUTE));
            assertNull(altar.endpoint.constellation);
        }
    }

    @Test
    void ritualSelectsItsConstellationWithoutRequiringStructureOrCrystal() {
        TestRitual ritual = attach(new TestRitual());
        assertFalse(ritual.hasMultiblock());
        assertEquals(200, insert(ritual, 200, Action.EXECUTE));
        assertNull(ritual.endpoint.constellation);
        ritual.constellation = new ConstellationBase.Weak("qio_ritual");
        assertEquals(200, insert(ritual, 200, Action.EXECUTE));
        assertSame(ritual.constellation, ritual.endpoint.constellation);
        assertEquals(2, ritual.endpoint.calls);
    }

    @Test
    void invalidWorldOrReplacedTargetCannotReceive() {
        TestReceiver target = new TestReceiver();
        assertFalse(ADAPTER.supports(target, EnumFacing.NORTH));
        TestWorld world = new TestWorld(false);
        world.attach(target);
        world.loaded = false;
        assertEquals(0, insert(target, 200, Action.EXECUTE));
        world.loaded = true;
        world.tile = null;
        assertEquals(0, insert(target, 200, Action.EXECUTE));
        world.attach(target);
        target.invalidate();
        assertEquals(0, insert(target, 200, Action.EXECUTE));
        TestReceiver client = new TestReceiver();
        new TestWorld(true).attach(client);
        assertEquals(0, insert(client, 200, Action.EXECUTE));
        assertEquals(0, target.fallback.calls);
        assertEquals(0, client.fallback.calls);
        assertFalse(ADAPTER.supports(attach(new TileEntity() {
        }), EnumFacing.NORTH));
    }

    @Test
    void missingEndpointsRefuseAndThrowingEndpointsReserveAnUnknownOutcome() {
        TestReceiver target = attach(new TestReceiver());
        target.fallback = null;
        assertEquals(0, insert(target, 200, Action.EXECUTE));
        target.fallback = new RecordingEndpoint(BlockPos.ORIGIN.up());
        assertEquals(0, insert(target, 200, Action.EXECUTE));
        assertEquals(0, target.fallback.calls);
        target.fallback = new RecordingEndpoint(target.getPos()) {
            @Override
            public void onStarlightReceive(World world, boolean loaded,
                  IWeakConstellation constellation, double amount) {
                throw new IllegalStateException("endpoint unavailable");
            }
        };
        assertEquals(200, insert(target, 200, Action.EXECUTE));
        assertTrue(mekceuqiostorage.common.integration.transfer.TransferRecovery.isBlocked(target));
        assertFalse(mekceuqiostorage.common.integration.transfer.TransferRecovery.pending(target).getBoolean("known"));
        assertEquals(0, insert(target, 200, Action.EXECUTE));
    }

    @Test
    void receiverCannotBeExtractedAndInvalidRequestsDoNotEmit() {
        TestReceiver target = attach(new TestReceiver());
        assertTrue(ADAPTER.getExtractable(target, EnumFacing.NORTH, 10, 200).isEmpty());
        assertEquals(0, ADAPTER.extract(target, EnumFacing.NORTH,
              QIOStorageDescriptors.starlight(), 200, Action.EXECUTE));
        assertEquals(0, ADAPTER.insert(target, EnumFacing.NORTH,
              QIOStorageDescriptors.mana(), 200, Action.EXECUTE));
        assertEquals(0, insert(target, 0, Action.EXECUTE));
        assertEquals(0, insert(target, -1, Action.EXECUTE));
        assertEquals(0, target.nodeLookups);
    }

    private static long insert(TileEntity target, long amount, Action action) {
        return ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.starlight(), amount, action);
    }

    private static <T extends TileEntity> T attach(T tile) {
        new TestWorld(false).attach(tile);
        return tile;
    }

    private static final class TestCrystal extends ItemTunedRockCrystal {
        private final IWeakConstellation constellation;

        private TestCrystal(IWeakConstellation constellation) {
            this.constellation = constellation;
        }

        @Override
        public IConstellation getFocusConstellation(ItemStack stack) {
            return constellation;
        }
    }

    private static final class TestAltar extends TileAltar {
        private RecordingEndpoint endpoint;

        private TestAltar(AltarLevel level) {
            super(level);
        }

        @Override
        public IPrismTransmissionNode getNode() {
            return endpoint;
        }

        @Override
        public void markForUpdate() {
        }

        @Override
        public void markDirty() {
        }
    }

    private static final class TestRitual extends TileRitualPedestal {
        private final RecordingEndpoint endpoint = new RecordingEndpoint(BlockPos.ORIGIN);
        private IWeakConstellation constellation;

        @Override
        public IWeakConstellation getRitualConstellation() {
            return constellation;
        }

        @Override
        public IPrismTransmissionNode getNode() {
            return endpoint;
        }

        @Override
        public void markDirty() {
        }
    }

    private static final class TestReceiver extends TileEntity implements IStarlightReceiver {
        private RecordingEndpoint node;
        private RecordingEndpoint fallback = new RecordingEndpoint(BlockPos.ORIGIN);
        private int nodeLookups;
        private int createdEndpoints;

        @Override
        public IPrismTransmissionNode getNode() {
            nodeLookups++;
            return node;
        }

        @Override
        public ITransmissionReceiver provideEndpoint(BlockPos pos) {
            createdEndpoints++;
            return fallback;
        }

        @Override
        public BlockPos getTrPos() {
            return getPos();
        }

        @Override
        public World getTrWorld() {
            return getWorld();
        }

        @Override
        public void markDirty() {
        }
    }

    private static class RecordingEndpoint extends SimpleTransmissionReceiver {
        private int calls;
        private double amount;
        private boolean chunkLoaded;
        private IWeakConstellation constellation;
        private World world;

        private RecordingEndpoint(BlockPos pos) {
            super(pos);
        }

        @Override
        public void onStarlightReceive(World world, boolean chunkLoaded,
              IWeakConstellation constellation, double amount) {
            calls++;
            this.world = world;
            this.chunkLoaded = chunkLoaded;
            this.constellation = constellation;
            this.amount = amount;
        }

        @Override
        public TransmissionClassRegistry.TransmissionProvider getProvider() {
            return null;
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
