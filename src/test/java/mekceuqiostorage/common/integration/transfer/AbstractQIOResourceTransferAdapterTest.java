package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import mekceuqiostorage.common.registration.QIOStorageRegistration;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractQIOResourceTransferAdapterTest {

    @BeforeAll
    static void registerCodecs() {
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void validatesAndResolvesMultiTypeTransfersBeforeCallingTheNativeApi() {
        TestAdapter adapter = new TestAdapter();
        TileEntity target = new TestTileEntity();
        QIOResourceDescriptor aer = QIOStorageDescriptors.essentia("aer");

        assertEquals(25, adapter.extract(target, EnumFacing.NORTH, aer, 25, Action.SIMULATE));
        assertEquals(Essentia.of("aer"), adapter.lastResource);
        assertSame(Action.SIMULATE, adapter.lastAction);
        assertEquals(1, adapter.calls);

        assertEquals(0, adapter.extract(target, EnumFacing.NORTH,
                QIOStorageDescriptors.mana(), 25, Action.EXECUTE));
        assertEquals(0, adapter.extract(target, EnumFacing.DOWN, aer, 25, Action.EXECUTE));
        assertEquals(0, adapter.extract(target, EnumFacing.NORTH, aer, -1, Action.EXECUTE));
        assertEquals(1, adapter.calls);
    }

    @Test
    void clampsInvalidNativeResultsAndRequiresAnAction() {
        TestAdapter adapter = new TestAdapter();
        TileEntity target = new TestTileEntity();
        QIOResourceDescriptor aer = QIOStorageDescriptors.essentia("aer");

        assertEquals(10, adapter.extract(target, EnumFacing.NORTH, aer, 10, Action.EXECUTE));
        assertEquals(0, adapter.insert(target, EnumFacing.NORTH, aer, 10, Action.EXECUTE));
        assertThrows(NullPointerException.class,
                () -> adapter.extract(target, EnumFacing.NORTH, aer, 10, null));
    }

    @Test
    void registrationRejectsAnAbsentOptionalProvider() {
        TestAdapter adapter = new TestAdapter();

        assertThrows(IllegalStateException.class,
                () -> QIOStorageRegistration.registerTransferAdapter(adapter));
    }

    private static final class TestAdapter
            extends AbstractQIOResourceTransferAdapter<Essentia> {

        private Essentia lastResource;
        private Action lastAction;
        private int calls;

        private TestAdapter() {
            super(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA);
        }

        @Override
        public boolean supports(TileEntity target, EnumFacing targetFace) {
            return targetFace == EnumFacing.NORTH;
        }

        @Override
        public List<QIOResourceStack> getExtractable(TileEntity target, EnumFacing targetFace,
                                                     int maximumTypes, long maximumAmount) {
            return Collections.emptyList();
        }

        @Override
        protected long extractResolved(TileEntity target, EnumFacing targetFace,
                                       Essentia resource, long amount, Action action) {
            lastResource = resource;
            lastAction = action;
            calls++;
            return amount == Long.MAX_VALUE ? amount : amount + 1;
        }

        @Override
        protected long insertResolved(TileEntity target, EnumFacing targetFace,
                                      Essentia resource, long amount, Action action) {
            return -1;
        }
    }

    private static final class TestTileEntity extends TileEntity {
    }
}
