package mekceuqiostorage.common;

import mekceuqiostorage.Tags;
import mekceuqiostorage.common.integration.QIOStorageHooks;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

/**
 * Stable identifiers shared by the common and client portions of the addon.
 */
public final class QIOStorage {

    public static final String MODID = Tags.MOD_ID;
    public static final String MOD_NAME = "Mekanism CEU QIO Storage";
    public static final String FORGE_DEPENDENCIES = "required-after:mekanism" +
          ";after:" + QIOStorageHooks.THAUMCRAFT +
          ";after:" + QIOStorageHooks.BOTANIA +
          ";after:" + QIOStorageHooks.BLOODMAGIC +
          ";after:" + QIOStorageHooks.ASTRALSORCERY +
          ";after:" + QIOStorageHooks.EMBERS +
          ";after:" + QIOStorageHooks.NATURESAURA +
          ";after:" + QIOStorageHooks.PNEUMATICCRAFT;
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    private QIOStorage() {
    }

    @Nonnull
    public static ResourceLocation id(@Nonnull String path) {
        return new ResourceLocation(MODID, path);
    }
}
