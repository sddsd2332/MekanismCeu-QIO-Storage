package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractSingleResourceTransferAdapterTest {

    private static final java.util.UUID LP_OWNER = java.util.UUID.fromString(
          "10000000-0000-0000-0000-000000000001");

    @BeforeAll
    static void registerCodecs() {
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void boundsCandidatesByTypeAndAmountBudgets() {
        TestAdapter adapter = new TestAdapter(125);
        TileEntity target = new TestTileEntity();

        assertTrue(adapter.getExtractable(target, EnumFacing.NORTH, 0, 100).isEmpty());
        assertTrue(adapter.getExtractable(target, EnumFacing.NORTH, 1, 0).isEmpty());
        List<QIOResourceStack> candidates = adapter.getExtractable(
                target, EnumFacing.NORTH, 1, 100);
        assertEquals(1, candidates.size());
        assertEquals(QIOStorageDescriptors.mana(), candidates.get(0).getDescriptor());
        assertEquals(100, candidates.get(0).getAmount());
    }

    @Test
    void simulationDoesNotMutateAndExecutionReturnsActualMovement() {
        TestAdapter adapter = new TestAdapter(125);
        TileEntity target = new TestTileEntity();
        QIOResourceDescriptor mana = QIOStorageDescriptors.mana();

        assertEquals(100, adapter.extract(target, EnumFacing.NORTH, mana, 100, Action.SIMULATE));
        assertEquals(125, adapter.stored);
        assertEquals(100, adapter.extract(target, EnumFacing.NORTH, mana, 100, Action.EXECUTE));
        assertEquals(25, adapter.stored);
        assertEquals(175, adapter.insert(target, EnumFacing.NORTH, mana, 300, Action.SIMULATE));
        assertEquals(25, adapter.stored);
        assertEquals(175, adapter.insert(target, EnumFacing.NORTH, mana, 300, Action.EXECUTE));
        assertEquals(200, adapter.stored);
    }

    @Test
    void rejectsWrongDescriptorsAndUnsupportedFaces() {
        TestAdapter adapter = new TestAdapter(100);
        TileEntity target = new TestTileEntity();

        assertEquals(0, adapter.extract(target, EnumFacing.NORTH,
                QIOStorageDescriptors.lp(LP_OWNER), 25, Action.EXECUTE));
        assertEquals(0, adapter.extract(target, EnumFacing.DOWN,
                QIOStorageDescriptors.mana(), 25, Action.EXECUTE));
        assertEquals(100, adapter.stored);
    }

    private static final class TestAdapter
            extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {

        private static final long CAPACITY = 200;
        private long stored;

        private TestAdapter(long stored) {
            super(QIOStorageResourceSpecs.BOTANIA_MANA, QIOStorageResources.MANA);
            this.stored = stored;
        }

        @Override
        public boolean supports(TileEntity target, EnumFacing targetFace) {
            return targetFace != EnumFacing.DOWN;
        }

        @Override
        protected long getStored(TileEntity target, EnumFacing targetFace) {
            return stored;
        }

        @Override
        protected long extractResource(TileEntity target, EnumFacing targetFace, long amount,
                                       Action action) {
            long moved = Math.min(stored, amount);
            if (action.execute()) {
                stored -= moved;
            }
            return moved;
        }

        @Override
        protected long insertResource(TileEntity target, EnumFacing targetFace, long amount,
                                      Action action) {
            long moved = Math.min(CAPACITY - stored, amount);
            if (action.execute()) {
                stored += moved;
            }
            return moved;
        }
    }

    private static final class TestTileEntity extends TileEntity {
    }
}
