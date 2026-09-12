package mekceuqiostorage.common.integration.pneumaticcraft;

import mekanism.api.Action;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IPneumaticMachine;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

@Tag("optional-mod-runtime")
class PneumaticCraftAirTransferAdapterTest {
    private static final PneumaticCraftAirTransferAdapter ADAPTER = PneumaticCraftAirTransferAdapter.INSTANCE;

    @BeforeAll static void register() { QIOStorageBootstrap.registerCodecs(); }

    @Test
    void exceptionsBeforePartialAndFullMutationSettleObservedInsertion() {
        for (int accepted : new int[]{0, 17, 64}) {
            Machine target = new Machine(100, accepted);
            long moved = ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE);
            assertEquals(accepted, moved);
            assertEquals(100 + accepted, target.air);
            assertEquals(1, target.calls);
            assertEquals(1100, 1000 - moved + target.air);
        }
    }

    @Test
    void exceptionsAfterExtractionDoNotLoseDebitedAir() {
        for (int accepted : new int[]{0, 17, 64}) {
            Machine source = new Machine(100, accepted);
            long moved = ADAPTER.extract(source, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.EXECUTE);
            assertEquals(accepted, moved);
            assertEquals(100, moved + source.air);
            assertEquals(1, source.calls);
        }
    }

    @Test
    void simulationDoesNotInvokeTheMutator() {
        Machine target = new Machine(100, 64);
        assertEquals(64, ADAPTER.insert(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.SIMULATE));
        assertEquals(64, ADAPTER.extract(target, EnumFacing.NORTH, QIOStorageDescriptors.air(), 64, Action.SIMULATE));
        assertEquals(0, target.calls);
        assertEquals(100, target.air);
    }

    private static final class Machine extends TileEntity implements IPneumaticMachine {
        private int air;
        private int calls;
        private final IAirHandler handler;

        Machine(int initial, int accepted) {
            air = initial;
            handler = (IAirHandler) Proxy.newProxyInstance(IAirHandler.class.getClassLoader(),
                  new Class<?>[]{IAirHandler.class}, (proxy, method, args) -> {
                      switch (method.getName()) {
                          case "getAir": return air;
                          case "getVolume": return 1000;
                          case "getMaxPressure": return 10F;
                          case "addAir":
                              calls++;
                              air += Integer.signum((Integer) args[0]) * accepted;
                              throw new IllegalStateException("injected native mutation failure");
                          case "toString": return "FaultAirHandler";
                          default: throw new UnsupportedOperationException(method.getName());
                      }
                  });
        }
        @Override public IAirHandler getAirHandler(EnumFacing face) { return handler; }
    }
}
