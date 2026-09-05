package mekceuqiostorage.client;

import mekceuqiostorage.client.integration.astralsorcery.AstralSorceryStarlightSelectionAdapter;
import mekceuqiostorage.client.integration.astralsorcery.AstralSorceryStarlightRenderer;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicLPRenderer;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicLPSelectionAdapter;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicWillRenderer;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicWillSelectionAdapter;
import mekceuqiostorage.client.integration.botania.BotaniaManaRenderer;
import mekceuqiostorage.client.integration.botania.BotaniaManaSelectionAdapter;
import mekceuqiostorage.client.integration.embers.EmbersEmberSelectionAdapter;
import mekceuqiostorage.client.integration.embers.EmbersEmberRenderer;
import mekceuqiostorage.client.integration.naturesaura.NaturesAuraSelectionAdapter;
import mekceuqiostorage.client.integration.pneumaticcraft.PneumaticCraftAirSelectionAdapter;
import mekceuqiostorage.client.integration.pneumaticcraft.PneumaticCraftAirRenderer;
import mekceuqiostorage.client.integration.thaumcraft.ThaumcraftEssentiaRenderer;
import mekceuqiostorage.client.integration.thaumcraft.ThaumcraftEssentiaSelectionAdapter;
import mekceuqiostorage.client.render.TextureQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.QIOStorageHooks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Forge-gated client renderer and container-selection registration.
 */
@SideOnly(Side.CLIENT)
public final class QIOStorageClientIntegrations {

    private static boolean registered;

    private QIOStorageClientIntegrations() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.THAUMCRAFT)) {
            registerThaumcraft();
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.BOTANIA)) {
            registerBotania();
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.BLOODMAGIC)) {
            registerBloodMagic();
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.ASTRALSORCERY)) {
            registerAstralSorcery();
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.EMBERS)) {
            registerEmbers();
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.NATURESAURA)) {
            registerNaturesAura();
        }
        if (QIOStorageHooks.isLoaded(QIOStorageHooks.PNEUMATICCRAFT)) {
            registerPneumaticCraft();
        }
        registered = true;
    }

    @Optional.Method(modid = QIOStorageHooks.THAUMCRAFT)
    private static void registerThaumcraft() {
        QIOStorageClientRegistration.registerRenderer(ThaumcraftEssentiaRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(
                ThaumcraftEssentiaSelectionAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.BOTANIA)
    private static void registerBotania() {
        QIOStorageClientRegistration.registerRenderer(BotaniaManaRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(BotaniaManaSelectionAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.BLOODMAGIC)
    private static void registerBloodMagic() {
        QIOStorageClientRegistration.registerRenderer(BloodMagicLPRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(BloodMagicLPSelectionAdapter.INSTANCE);
        QIOStorageClientRegistration.registerRenderer(BloodMagicWillRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(BloodMagicWillSelectionAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.ASTRALSORCERY)
    private static void registerAstralSorcery() {
        QIOStorageClientRegistration.registerRenderer(AstralSorceryStarlightRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(
                AstralSorceryStarlightSelectionAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.EMBERS)
    private static void registerEmbers() {
        QIOStorageClientRegistration.registerRenderer(EmbersEmberRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(EmbersEmberSelectionAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.NATURESAURA)
    private static void registerNaturesAura() {
        registerScalar(QIOStorageResourceSpecs.NATURESAURA_AURA, QIOStorageResources.AURA,
                "naturesaura", "textures/items/aura_cache.png", "naturesaura_aura");
        QIOStorageClientRegistration.registerSelectionAdapter(NaturesAuraSelectionAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.PNEUMATICCRAFT)
    private static void registerPneumaticCraft() {
        QIOStorageClientRegistration.registerRenderer(PneumaticCraftAirRenderer.INSTANCE);
        QIOStorageClientRegistration.registerSelectionAdapter(
                PneumaticCraftAirSelectionAdapter.INSTANCE);
    }

    private static void registerScalar(
            mekceuqiostorage.common.content.qio.QIOStorageResourceSpec<QIOStorageResources.Scalar> spec,
            QIOStorageResources.Scalar resource, String textureNamespace, String texturePath,
            String keySuffix) {
        QIOStorageClientRegistration.registerRenderer(new TextureQIOResourceRenderer<>(spec,
                resource, new ResourceLocation(textureNamespace, texturePath),
                "qio.mekceuqiostorage.resource." + keySuffix,
                "qio.mekceuqiostorage.source." + textureNamespace,
                "qio.mekceuqiostorage.tooltip.unit." + keySuffix));
    }
}
