package mekceuqiostorage.common.integration.botania;

import mekanism.api.Action;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import mekceuqiostorage.common.integration.transfer.NativeTransferAccounting;
import net.minecraft.tileentity.TileEntity;
import vazkii.botania.api.mana.IManaCollector;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.mana.spark.ISparkAttachable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Write-only endpoint for Botania receivers that are not direction-switchable mana pools. */
final class BotaniaManaReceiverEndpoint {

    private BotaniaManaReceiverEndpoint() {
    }

    static boolean supports(@Nonnull TileEntity target) {
        if (!(target instanceof IManaReceiver) || target instanceof IManaPool) {
            return false;
        }
        IManaReceiver receiver = (IManaReceiver) target;
        return readCurrentMana(receiver) != null && readFull(receiver) != null &&
              readCanReceive(receiver) != null;
    }

    static long insert(@Nonnull TileEntity target, long amount, @Nonnull Action action) {
        if (!supports(target)) {
            return 0;
        }
        IManaReceiver receiver = (IManaReceiver) target;
        Integer beforeValue = readCurrentMana(receiver);
        Boolean full = readFull(receiver);
        Boolean canReceive = readCanReceive(receiver);
        if (beforeValue == null || full == null || full || canReceive == null || !canReceive) {
            return 0;
        }
        int before = beforeValue;
        int requested = QIOStorageTransferMath.intLimit(
              QIOStorageTransferMath.limit(amount, availableSpace(receiver, before)));
        if (requested <= 0 || action.simulate()) {
            return requested;
        }
        if (receiver instanceof IManaCollector || receiver instanceof ISparkAttachable) {
            return NativeTransferAccounting.observed(requested, true, receiver::getCurrentMana,
                  () -> receiver.recieveMana(requested), null);
        }
        // The base interface can represent a router or a consuming sink. Its balance cannot
        // establish whether a throwing invocation forwarded a payload elsewhere.
        return NativeTransferAccounting.reported(requested, true, null, () -> {
            receiver.recieveMana(requested);
            Integer after = readCurrentMana(receiver);
            long retained = after == null ? 0 : QIOStorageTransferMath.increase(before, after);
            return retained > 0 ? QIOStorageTransferMath.result(retained, requested) : requested;
        }, null);
    }

    private static long availableSpace(IManaReceiver receiver, int current) {
        try {
            if (receiver instanceof ISparkAttachable) {
                return QIOStorageTransferMath.nonNegativeInt(
                      ((ISparkAttachable) receiver).getAvailableSpaceForMana());
            }
            if (receiver instanceof IManaCollector) {
                int maximum = ((IManaCollector) receiver).getMaxMana();
                return maximum >= current ? maximum - (long) current : 0;
            }
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        // The base interface exposes no capacity. Keep addition in the signed int range; the
        // receiver's post-call balance remains authoritative for buffered implementations.
        return Integer.MAX_VALUE - (long) current;
    }

    @Nullable
    private static Integer readCurrentMana(IManaReceiver receiver) {
        try {
            int current = receiver.getCurrentMana();
            return current < 0 ? null : current;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean readFull(IManaReceiver receiver) {
        try {
            return receiver.isFull();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean readCanReceive(IManaReceiver receiver) {
        try {
            return receiver.canRecieveManaFromBursts();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }
}
