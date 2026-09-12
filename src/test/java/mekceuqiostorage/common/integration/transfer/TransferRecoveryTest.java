package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.common.content.qio.QIOFrequency;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferRecoveryTest {
    private static final TestAdapter ADAPTER = new TestAdapter();

    @BeforeAll static void register() {
        Bootstrap.register();
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void unknownInsertionIsReservedAndCannotBeRepeatedOrAutomaticallyInferredLater() {
        Target target = target();
        target.hideAfter = true;
        TestFrequency frequency = new TestFrequency();
        long charged = ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE);
        frequency.amount -= charged;
        assertEquals(64, charged);
        assertEquals(117, target.balance);
        NBTTagCompound record = TransferRecovery.pending(target);
        assertFalse(record.getBoolean("known"));
        assertEquals(64, record.getLong("settled"));
        target.hideAfter = false;
        assertEquals(0, ADAPTER.insert(target, EnumFacing.SOUTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE));
        assertEquals(0, ADAPTER.extract(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.SIMULATE));
        assertEquals(1, target.calls);
        assertFalse(TransferRecovery.pending(target).getBoolean("known"));
        TransferRecovery journal = TransferRecovery.get(target.getWorld());
        // Test fixture has independent knowledge of the original mutation; the later getter is not used.
        assertEquals(0, journal.recover(record.getString("id"), 17, frequency));
        assertEquals(1100, frequency.amount + target.balance);
        assertFalse(TransferRecovery.isBlocked(target));
        assertEquals(0, journal.recover(record.getString("id"), 17, frequency));
        assertEquals(1100, frequency.amount + target.balance);
    }

    @Test
    void unknownExtractionRetainsItsClaimAcrossSerializationAndTileReplacement() {
        Target source = target();
        source.hideAfter = true;
        TestFrequency frequency = new TestFrequency();
        assertEquals(0, ADAPTER.extract(source, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE));
        assertEquals(83, source.balance);
        TransferRecovery journal = TransferRecovery.get(source.getWorld());
        String id = TransferRecovery.pending(source).getString("id");
        NBTTagCompound serialized = journal.writeToNBT(new NBTTagCompound());
        TransferRecovery restored = new TransferRecovery();
        restored.readFromNBT(serialized);
        source.invalidate();
        Target replacement = new Target();
        replacement.setWorld(source.getWorld());
        assertFalse(TransferRecovery.isBlocked(replacement));
        assertEquals(1, restored.list().size());
        assertEquals(0, restored.recover(id, 17, frequency));
        assertEquals(1100, frequency.amount + source.balance);
        assertEquals(0, restored.recover(id, 17, frequency));
        assertEquals(1100, frequency.amount + source.balance);
    }

    @Test
    void failedCompensationIsAnExplicitSignedClaimAndItsSettlementCanBeResumed() {
        for (boolean insertion : new boolean[]{true, false}) {
            Target target = target();
            target.movement = 80;
            target.compensationFails = true;
            TestFrequency frequency = new TestFrequency();
            long settled = insertion ? ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE) :
                  ADAPTER.extract(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE);
            frequency.amount += insertion ? -settled : settled;
            NBTTagCompound record = TransferRecovery.pending(target);
            assertTrue(record.getBoolean("known"));
            assertEquals(80, record.getLong("actual"));
            assertEquals(1100, frequency.amount + target.balance + record.getLong("remaining"));
            assertEquals(2, target.calls);
            TransferRecovery journal = TransferRecovery.get(target.getWorld());
            frequency.limit = 5;
            long remaining = journal.recover(record.getString("id"), 80, frequency);
            assertNotEquals(0, remaining);
            assertEquals(1100, frequency.amount + target.balance + remaining);
            assertThrows(IllegalArgumentException.class, () -> journal.recover(record.getString("id"), 64, frequency));
            frequency.limit = Long.MAX_VALUE;
            assertEquals(0, journal.recover(record.getString("id"), 80, frequency));
            assertEquals(1100, frequency.amount + target.balance);
            assertEquals(2, target.calls);
        }
    }

    @Test
    void throwingCompensationThatActuallyRestoresIsVerifiedBeforeRefunding() {
        Target target = target();
        target.movement = 80;
        assertEquals(0, ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE));
        assertEquals(100, target.balance);
        assertEquals(2, target.calls);
        assertTrue(TransferRecovery.pending(target).isEmpty());
    }

    @Test
    void transientPostReadFailureRetriesOnlyTheRead() {
        Target target = target();
        target.failedReads = 1;
        assertEquals(17, ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE));
        assertEquals(117, target.balance);
        assertEquals(1, target.calls);
        assertTrue(TransferRecovery.pending(target).isEmpty());
    }

    @Test
    void simulationsDoNotCreateClaimsOrModifyNativeState() {
        Target target = target();
        NBTTagCompound before = target.getTileData().copy();
        assertEquals(64, ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.SIMULATE));
        assertEquals(64, ADAPTER.extract(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.SIMULATE));
        assertEquals(before, target.getTileData());
        assertEquals(100, target.balance);
        assertEquals(0, target.calls);
    }

    private static Target target() {
        Target target = new Target();
        target.setWorld(new TestWorld());
        return target;
    }

    private static final class Target extends TileEntity {
        double balance = 100;
        int movement = 17;
        int calls;
        int failedReads;
        boolean hideAfter;
        boolean compensationFails;
        double read() {
            if (calls > 0 && (hideAfter || failedReads-- > 0)) throw new IllegalStateException("post-read failure");
            return balance;
        }
        void change(double delta) {
            calls++;
            if (calls > 1 && compensationFails) throw new IllegalStateException("compensation refused");
            balance += calls == 1 ? Math.signum(delta) * movement : delta;
            throw new IllegalStateException("failure after mutation");
        }
        @Override public void markDirty() { }
    }

    private static final class TestAdapter extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {
        TestAdapter() { super(QIOStorageResourceSpecs.PNEUMATICCRAFT_AIR, QIOStorageResources.AIR); }
        @Override public boolean supports(TileEntity target, EnumFacing face) { return target instanceof Target; }
        @Override protected long getStored(TileEntity target, EnumFacing face) { return (long) ((Target) target).read(); }
        @Override protected long insertResource(TileEntity target, EnumFacing face, long amount, Action action) {
            return transfer((Target) target, amount, action, true);
        }
        @Override protected long extractResource(TileEntity target, EnumFacing face, long amount, Action action) {
            return transfer((Target) target, amount, action, false);
        }
        private long transfer(Target target, long amount, Action action, boolean insertion) {
            return action.simulate() ? amount : NativeTransferAccounting.observed(amount, insertion, target::read,
                  () -> target.change(insertion ? amount : -amount), target::change);
        }
    }

    private static final class TestWorld extends World {
        TestWorld() { super(null, new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
    }

    private static final class TestFrequency extends QIOFrequency {
        long amount = 1000;
        long limit = Long.MAX_VALUE;
        TestFrequency() { setValid(true); }
        @Override public long massInsert(QIOResourceDescriptor descriptor, long requested, Action action) {
            long moved = Math.min(limit, requested);
            if (action.execute()) amount += moved;
            return moved;
        }
        @Override public long massExtract(QIOResourceDescriptor descriptor, long requested, Action action) {
            long moved = Math.min(amount, Math.min(limit, requested));
            if (action.execute()) amount -= moved;
            return moved;
        }
    }
}
