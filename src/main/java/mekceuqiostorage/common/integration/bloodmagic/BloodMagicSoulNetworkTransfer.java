package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.core.data.SoulNetwork;
import WayofTime.bloodmagic.core.data.SoulTicket;
import mekanism.api.Action;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import mekceuqiostorage.common.integration.transfer.NativeTransferAccounting;

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
        return NativeTransferAccounting.observed(requested, false, network::getCurrentEssence,
              () -> network.syphon(ticketFactory.create(requested)), change -> compensate(network, change));
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
        return NativeTransferAccounting.observed(requested, true, network::getCurrentEssence,
              () -> network.add(ticketFactory.create(requested), maximum), change -> compensate(network, change));
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

    private static void compensate(SoulNetwork network, double change) {
        if (change > 0) network.add(new SoulTicket((int) change), Integer.MAX_VALUE);
        else if (change < 0) network.syphon(new SoulTicket((int) -change));
    }

    interface TicketFactory {

        @Nonnull
        SoulTicket create(int amount);
    }
}
