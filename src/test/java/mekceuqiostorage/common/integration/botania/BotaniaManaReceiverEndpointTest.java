package mekceuqiostorage.common.integration.botania;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.mana.IManaCollector;
import vazkii.botania.api.mana.IManaReceiver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("optional-mod-runtime")
class BotaniaManaReceiverEndpointTest {

    @BeforeAll
    static void registerCodecs() {
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void genericBufferedReceiverIsWriteOnlyAndReportsItsObservedDelta() {
        TestReceiver receiver = new TestReceiver(80, 100);
        QIOResourceDescriptor mana = QIOStorageDescriptors.mana();

        assertTrue(BotaniaManaTransferAdapter.INSTANCE.supports(receiver, EnumFacing.NORTH));
        assertTrue(BotaniaManaTransferAdapter.INSTANCE.getExtractable(
              receiver, EnumFacing.NORTH, 1, 64).isEmpty());
        assertEquals(0, BotaniaManaTransferAdapter.INSTANCE.extract(
              receiver, EnumFacing.NORTH, mana, 64, Action.EXECUTE));

        assertEquals(64, BotaniaManaTransferAdapter.INSTANCE.insert(
              receiver, EnumFacing.NORTH, mana, 64, Action.SIMULATE));
        assertEquals(80, receiver.stored);
        assertEquals(20, BotaniaManaTransferAdapter.INSTANCE.insert(
              receiver, EnumFacing.NORTH, mana, 64, Action.EXECUTE));
        assertEquals(100, receiver.stored);
    }

    @Test
    void collectorCapacityBoundsSimulationBeforeQioExtractsMana() {
        TestCollector receiver = new TestCollector(80, 100);
        QIOResourceDescriptor mana = QIOStorageDescriptors.mana();

        assertEquals(20, BotaniaManaTransferAdapter.INSTANCE.insert(
              receiver, EnumFacing.UP, mana, 64, Action.SIMULATE));
        assertEquals(80, receiver.stored);
        assertEquals(20, BotaniaManaTransferAdapter.INSTANCE.insert(
              receiver, EnumFacing.UP, mana, 64, Action.EXECUTE));
        assertEquals(100, receiver.stored);
    }

    @Test
    void statelessReceiverUsesBotaniasDeliveredPayloadContract() {
        TestSink receiver = new TestSink();
        QIOResourceDescriptor mana = QIOStorageDescriptors.mana();

        assertEquals(64, BotaniaManaTransferAdapter.INSTANCE.insert(
              receiver, EnumFacing.DOWN, mana, 64, Action.SIMULATE));
        assertEquals(0, receiver.received);
        assertEquals(64, BotaniaManaTransferAdapter.INSTANCE.insert(
              receiver, EnumFacing.DOWN, mana, 64, Action.EXECUTE));
        assertEquals(64, receiver.received);
    }

    @Test
    void currentAcceptanceStateDoesNotChangeInterfaceSupport() {
        TestReceiver full = new TestReceiver(100, 100);
        TestReceiver disabled = new TestReceiver(0, 100);
        disabled.canReceive = false;
        QIOResourceDescriptor mana = QIOStorageDescriptors.mana();

        assertTrue(BotaniaManaTransferAdapter.INSTANCE.supports(full, EnumFacing.NORTH));
        assertTrue(BotaniaManaTransferAdapter.INSTANCE.supports(disabled, EnumFacing.NORTH));
        assertEquals(0, BotaniaManaTransferAdapter.INSTANCE.insert(
              full, EnumFacing.NORTH, mana, 64, Action.EXECUTE));
        assertEquals(0, BotaniaManaTransferAdapter.INSTANCE.insert(
              disabled, EnumFacing.NORTH, mana, 64, Action.EXECUTE));
        assertFalse(BotaniaManaTransferAdapter.INSTANCE.supports(
              new PlainTileEntity(), EnumFacing.NORTH));
    }

    private static final class PlainTileEntity extends TileEntity {
    }

    private static class TestReceiver extends TileEntity implements IManaReceiver {

        protected int stored;
        protected final int capacity;
        protected boolean canReceive = true;

        private TestReceiver(int stored, int capacity) {
            this.stored = stored;
            this.capacity = capacity;
        }

        @Override
        public int getCurrentMana() {
            return stored;
        }

        @Override
        public boolean isFull() {
            return stored >= capacity;
        }

        @Override
        public void recieveMana(int mana) {
            stored = Math.min(capacity, stored + mana);
        }

        @Override
        public boolean canRecieveManaFromBursts() {
            return canReceive;
        }
    }

    private static final class TestCollector extends TestReceiver implements IManaCollector {

        private TestCollector(int stored, int capacity) {
            super(stored, capacity);
        }

        @Override
        public void onClientDisplayTick() {
        }

        @Override
        public float getManaYieldMultiplier(IManaBurst burst) {
            return 1;
        }

        @Override
        public int getMaxMana() {
            return capacity;
        }
    }

    private static final class TestSink extends TileEntity implements IManaReceiver {

        private int received;

        @Override
        public int getCurrentMana() {
            return 0;
        }

        @Override
        public boolean isFull() {
            return false;
        }

        @Override
        public void recieveMana(int mana) {
            received += mana;
        }

        @Override
        public boolean canRecieveManaFromBursts() {
            return true;
        }
    }
}
