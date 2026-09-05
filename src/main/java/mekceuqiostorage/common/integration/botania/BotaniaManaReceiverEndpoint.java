package mekceuqiostorage.common.integration.botania;

import mekanism.api.Action;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
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
        try {
            receiver.recieveMana(requested);
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        Integer afterValue = readCurrentMana(receiver);
        if (afterValue == null) {
            // The void method returned normally, so follow Botania's own burst-delivery contract
            // when a receiver cannot expose a usable post-state.
            return requested;
        }
        long retained = QIOStorageTransferMath.increase(before, afterValue);
        if (retained > 0) {
            return QIOStorageTransferMath.result(retained, requested);
        }
        // Routers, converters and sinks legitimately retain no balance (or may consume an older
        // balance immediately). Native mana bursts treat a successful call as full delivery.
        return requested;
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
