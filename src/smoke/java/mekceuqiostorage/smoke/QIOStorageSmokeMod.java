package mekceuqiostorage.smoke;

import mekanism.api.qio.resource.QIOResourceCodecRegistry;
import mekanism.api.qio.resource.QIOResourceTransferAdapterRegistry;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.registration.QIOStorageItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mod(modid = "mekceuqiostorage_smoke", name = "QIO Storage Development Checks", version = "1",
      dependencies = "required-after:mekceuqiostorage")
public final class QIOStorageSmokeMod {

    private int ticks;
    private boolean finished;

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void clientLoaded(FMLLoadCompleteEvent event) {
        String result;
        try {
            QIOStorageClientSmokeTest.verify();
            result = "PASS client " + System.getProperty("mekceuqiostorage.smoke.integration", "as") +
                  " renderer and drive selector registration";
        } catch (Throwable failure) {
            QIOStorage.LOGGER.error("QIO client smoke verification failed", failure);
            result = "FAIL " + failure;
        }
        writeResult(result);
        Minecraft.getMinecraft().shutdown();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END || ++ticks < 20) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        String result;
        try {
            if ("lp".equals(System.getProperty("mekceuqiostorage.smoke.integration")) &&
                  Loader.isModLoaded("bloodmagic")) {
                if (!checkLp(server, ticks - 20)) {
                    return;
                }
                finished = true;
                writeResult("PASS LP native altar import/export, owner filters and drive persistence");
                server.initiateShutdown();
                return;
            }
            boolean will = "will".equals(System.getProperty("mekceuqiostorage.smoke.integration"));
            if (Loader.isModLoaded(will ? "bloodmagic" : "astralsorcery")) {
                if (!(will ? checkWill(server, ticks - 20) : checkAstralSorcery(server, ticks - 20))) {
                    return;
                }
                if (will) {
                    result = Boolean.getBoolean("mekceuqiostorage.smoke.reload") ?
                          "PASS all five Will types persisted after restart" :
                          "PASS independent LP/Will drives, five-type import/export, filters, remainders and capacity";
                } else {
                    result = Boolean.getBoolean("mekceuqiostorage.smoke.reload") ?
                          "PASS AS persisted QIO inventory after restart" :
                          "PASS AS drives, native importer, exporter direction, filters, full altar and no-op receiver";
                }
            } else {
                check(QIOResourceCodecRegistry.INSTANCE.get(QIOStorageCodecs.ASTRALSORCERY_STARLIGHT.getCodecId()) ==
                      QIOStorageCodecs.ASTRALSORCERY_STARLIGHT, "Persisted AS codec must remain available");
                check(QIOResourceTransferAdapterRegistry.INSTANCE.get(
                      QIOStorageCodecs.ASTRALSORCERY_STARLIGHT.getCodecId()) == null, "AS adapter loaded without AS");
                check(QIOStorageItems.getRegisteredItems().isEmpty(), "Optional drives loaded without providers");
                check(QIOResourceCodecRegistry.INSTANCE.get(QIOStorageCodecs.BLOODMAGIC_WILL.getCodecId()) ==
                      QIOStorageCodecs.BLOODMAGIC_WILL, "Persisted Will codec must remain available");
                check(QIOResourceTransferAdapterRegistry.INSTANCE.get(QIOStorageCodecs.BLOODMAGIC_WILL.getCodecId()) == null,
                      "Will adapter loaded without Blood Magic");
                result = "PASS no optional providers: codecs retained, drives and adapters absent";
            }
        } catch (Throwable failure) {
            QIOStorage.LOGGER.error("QIO smoke verification failed", failure);
            result = "FAIL " + failure;
        }
        finished = true;
        writeResult(result);
        server.initiateShutdown();
    }

    private static void writeResult(String result) {
        QIOStorage.LOGGER.info(result);
        try {
            Files.write(Paths.get(System.getProperty("mekceuqiostorage.smoke.result")),
                  result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            QIOStorage.LOGGER.error("Cannot write QIO smoke result", failure);
        }
    }

    @Optional.Method(modid = "astralsorcery")
    private static boolean checkAstralSorcery(MinecraftServer server, int tick) {
        return AstralSorcerySmokeTest.tick(server.getWorld(0), tick);
    }

    @Optional.Method(modid = "bloodmagic")
    private static boolean checkWill(MinecraftServer server, int tick) {
        return BloodMagicWillSmokeTest.tick(server.getWorld(0), tick);
    }

    @Optional.Method(modid = "bloodmagic")
    private static boolean checkLp(MinecraftServer server, int tick) throws Exception {
        return BloodMagicLPSmokeTest.tick(server.getWorld(0), tick);
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
