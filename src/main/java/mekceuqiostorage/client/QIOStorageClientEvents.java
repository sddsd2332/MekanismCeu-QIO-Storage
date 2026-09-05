package mekceuqiostorage.client;

import mekceuqiostorage.common.MekanismCeuQIOStorage;
import mekceuqiostorage.common.QIOStorage;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Keeps Forge client events out of the common mod entrypoint's class signature.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = QIOStorage.MODID)
public final class QIOStorageClientEvents {

    private QIOStorageClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (MekanismCeuQIOStorage.proxy != null) {
            MekanismCeuQIOStorage.proxy.registerItemRenders();
        }
    }
}
