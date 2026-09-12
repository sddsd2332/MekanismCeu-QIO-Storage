package mekceuqiostorage.common.integration.transfer;

import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.integration.thaumcraft.ThaumcraftEssentiaTransferAdapter;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = QIOStorage.MODID)
public final class TransferLifecycleEvents {
    private TransferLifecycleEvents() { }

    @SubscribeEvent
    public static void unloadChunk(ChunkEvent.Unload event) {
        if (Loader.isModLoaded("thaumcraft")) forgetChunk(event);
    }

    @SubscribeEvent
    public static void unloadWorld(WorldEvent.Unload event) {
        if (Loader.isModLoaded("thaumcraft")) forgetWorld(event);
    }

    @Optional.Method(modid = "thaumcraft")
    private static void forgetChunk(ChunkEvent.Unload event) {
        event.getChunk().getTileEntityMap().values().forEach(ThaumcraftEssentiaTransferAdapter::forget);
    }

    @Optional.Method(modid = "thaumcraft")
    private static void forgetWorld(WorldEvent.Unload event) {
        ThaumcraftEssentiaTransferAdapter.forgetWorld(event.getWorld());
    }
}
