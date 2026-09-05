package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.core.data.BMWorldSavedData;
import WayofTime.bloodmagic.core.data.SoulNetwork;
import WayofTime.bloodmagic.core.data.SoulTicket;
import mekanism.api.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodMagicSoulNetworkTransferTest {

    private static final UUID OWNER = UUID.fromString(
          "10000000-0000-0000-0000-000000000001");

    private SoulNetwork network;

    @BeforeEach
    void createNetwork() {
        network = new BMWorldSavedData().getNetwork(OWNER);
    }

    @Test
    void simulationsRespectLimitsWithoutCreatingTicketsOrChangingLp() {
        network.setCurrentEssence(100);
        AtomicInteger createdTickets = new AtomicInteger();
        BloodMagicSoulNetworkTransfer.TicketFactory tickets = amount -> {
            createdTickets.incrementAndGet();
            return new SoulTicket(amount);
        };

        assertEquals(40, BloodMagicSoulNetworkTransfer.getExtractable(network, 40));
        assertEquals(40, BloodMagicSoulNetworkTransfer.extract(network, 200, 40,
              Action.SIMULATE, tickets));
        assertEquals(30, BloodMagicSoulNetworkTransfer.insert(network, 200, 150, 30,
              Action.SIMULATE, tickets));
        assertEquals(100, network.getCurrentEssence());
        assertEquals(0, createdTickets.get());
    }

    @Test
    void executionsMoveTheBoundedAmountReportedByTheNetwork() {
        network.setCurrentEssence(100);

        assertEquals(40, BloodMagicSoulNetworkTransfer.extract(network, 200, 40,
              Action.EXECUTE, SoulTicket::new));
        assertEquals(60, network.getCurrentEssence());
        assertEquals(30, BloodMagicSoulNetworkTransfer.insert(network, 200, 90, 50,
              Action.EXECUTE, SoulTicket::new));
        assertEquals(90, network.getCurrentEssence());
        assertEquals(0, BloodMagicSoulNetworkTransfer.insert(network, 1, 90, 50,
              Action.EXECUTE, SoulTicket::new));
    }

    @Test
    void providerRefusalLeavesTheNetworkUnchanged() {
        network.setCurrentEssence(100);

        assertEquals(0, BloodMagicSoulNetworkTransfer.extract(network, 40, 40,
              Action.EXECUTE, amount -> new SoulTicket(0)));
        assertEquals(100, network.getCurrentEssence());
        assertEquals(0, BloodMagicSoulNetworkTransfer.insert(network, 40, 200, 40,
              Action.EXECUTE, amount -> new SoulTicket(0)));
        assertEquals(100, network.getCurrentEssence());
    }

    @Test
    void providerAmountsAreMeasuredAndNeverOverReported() {
        network.setCurrentEssence(100);

        assertEquals(20, BloodMagicSoulNetworkTransfer.extract(network, 20, 20,
              Action.EXECUTE, amount -> new SoulTicket(amount + 25)));
        assertEquals(80, network.getCurrentEssence());
        assertEquals(20, BloodMagicSoulNetworkTransfer.insert(network, 20, 200, 20,
              Action.EXECUTE, amount -> new SoulTicket(amount + 25)));
        assertEquals(100, network.getCurrentEssence());
    }

    @Test
    void providerFailureRestoresThePreTransferBalance() {
        network.setCurrentEssence(100);

        assertEquals(0, BloodMagicSoulNetworkTransfer.extract(network, 20, 20,
              Action.EXECUTE, amount -> {
                  network.setCurrentEssence(5);
                  throw new IllegalStateException("simulated provider failure");
              }));
        assertEquals(100, network.getCurrentEssence());
        assertEquals(0, BloodMagicSoulNetworkTransfer.insert(network, 20, 200, 20,
              Action.EXECUTE, amount -> {
                  network.setCurrentEssence(175);
                  throw new IllegalStateException("simulated provider failure");
              }));
        assertEquals(100, network.getCurrentEssence());
    }

    @Test
    void altarLimitsUseBloodMagicsOrbCapacityAndFillRateUnits() {
        assertEquals(150, BloodMagicSoulNetworkEndpoint.maximum(100, 1.5F));
        assertEquals(300, BloodMagicSoulNetworkEndpoint.transferLimit(20, 0.5F));
        assertEquals(Integer.MAX_VALUE,
              BloodMagicSoulNetworkEndpoint.maximum(Integer.MAX_VALUE, 2F));
        assertEquals(Integer.MAX_VALUE,
              BloodMagicSoulNetworkEndpoint.transferLimit(Integer.MAX_VALUE, 1F));
        assertEquals(0, BloodMagicSoulNetworkEndpoint.maximum(100, 0F));
        assertEquals(0, BloodMagicSoulNetworkEndpoint.maximum(100, Float.NaN));
        assertEquals(0, BloodMagicSoulNetworkEndpoint.transferLimit(20, -0.1F));
        assertEquals(0, BloodMagicSoulNetworkEndpoint.transferLimit(20,
              Float.POSITIVE_INFINITY));
    }
}
