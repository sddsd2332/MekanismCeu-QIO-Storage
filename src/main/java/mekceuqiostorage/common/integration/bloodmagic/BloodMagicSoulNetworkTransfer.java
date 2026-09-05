package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.core.data.SoulNetwork;
import WayofTime.bloodmagic.core.data.SoulTicket;
import mekanism.api.Action;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Transactional access to Blood Magic's event-aware Soul Network operations. */
final class BloodMagicSoulNetworkTransfer {

    private BloodMagicSoulNetworkTransfer() {
    }

    static long getExtractable(@Nullable SoulNetwork network, int transferLimit) {
        Integer current = readCurrent(network);
        return current == null ? 0 : QIOStorageTransferMath.limit(current, transferLimit);
    }

    static long extract(@Nullable SoulNetwork network, long amount, int transferLimit,
          @Nonnull Action action, @Nonnull TicketFactory ticketFactory) {
        Integer beforeValue = readCurrent(network);
        if (beforeValue == null) {
            return 0;
        }
        int before = beforeValue;
        int requested = QIOStorageTransferMath.intLimit(QIOStorageTransferMath.limit(amount,
              Math.min(before, transferLimit)));
        if (requested <= 0 || action.simulate()) {
            return requested;
        }
        try {
            network.syphon(ticketFactory.create(requested));
        } catch (LinkageError | RuntimeException ignored) {
            restore(network, before);
            return 0;
        }
        Integer afterValue = readCurrent(network);
        if (afterValue == null || afterValue > before) {
            restore(network, before);
            return 0;
        }
        long moved = QIOStorageTransferMath.decrease(before, afterValue);
        if (moved > requested) {
            restore(network, before - requested);
            return requested;
        }
        return moved;
    }

    static long insert(@Nullable SoulNetwork network, long amount, int maximum, int transferLimit,
          @Nonnull Action action, @Nonnull TicketFactory ticketFactory) {
        Integer beforeValue = readCurrent(network);
        if (beforeValue == null || maximum <= beforeValue) {
            return 0;
        }
        int before = beforeValue;
        int requested = QIOStorageTransferMath.intLimit(QIOStorageTransferMath.limit(amount,
              Math.min(maximum - (long) before, transferLimit)));
        if (requested <= 0 || action.simulate()) {
            return requested;
        }
        try {
            network.add(ticketFactory.create(requested), maximum);
        } catch (LinkageError | RuntimeException ignored) {
            restore(network, before);
            return 0;
        }
        Integer afterValue = readCurrent(network);
        if (afterValue == null || afterValue < before) {
            restore(network, before);
            return 0;
        }
        long moved = QIOStorageTransferMath.increase(before, afterValue);
        if (moved > requested) {
            restore(network, before + requested);
            return requested;
        }
        return moved;
    }

    @Nullable
    private static Integer readCurrent(@Nullable SoulNetwork network) {
        if (network == null) {
            return null;
        }
        try {
            int current = network.getCurrentEssence();
            return current < 0 ? null : current;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static void restore(SoulNetwork network, int amount) {
        try {
            network.setCurrentEssence(Math.max(0, amount));
        } catch (LinkageError | RuntimeException ignored) {
            // Best-effort restoration after a provider event mutates an operation unexpectedly.
        }
    }

    interface TicketFactory {

        @Nonnull
        SoulTicket create(int amount);
    }
}
