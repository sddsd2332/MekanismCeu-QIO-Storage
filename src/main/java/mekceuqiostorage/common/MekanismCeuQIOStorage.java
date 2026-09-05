package mekceuqiostorage.common;

import mekceuqiostorage.Tags;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import mekceuqiostorage.common.registration.QIOStorageItems;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Entrypoint for the Mekanism CEU QIO resource storage addon.
 */
@Mod(modid = QIOStorage.MODID, useMetadata = true, acceptedMinecraftVersions = "[1.12,1.13)",
      version = Tags.VERSION, dependencies = QIOStorage.FORGE_DEPENDENCIES)
@Mod.EventBusSubscriber(modid = QIOStorage.MODID)
public class MekanismCeuQIOStorage {

    @SidedProxy(clientSide = "mekceuqiostorage.client.QIOStorageClientProxy",
            serverSide = "mekceuqiostorage.common.QIOStorageCommonProxy")
    public static QIOStorageCommonProxy proxy;

    @Mod.Instance(QIOStorage.MODID)
    public static MekanismCeuQIOStorage instance;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        QIOStorageItems.registerItems(event.getRegistry());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (proxy != null) {
            proxy.preInit();
        } else {
            QIOStorageBootstrap.initialize();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (proxy != null) {
            proxy.registerClientHandlers();
        }
    }
}
