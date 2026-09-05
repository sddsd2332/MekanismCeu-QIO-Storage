package mekceuqiostorage.common.integration;

import mekceuqiostorage.common.integration.astralsorcery.AstralSorceryIntegration;
import mekceuqiostorage.common.integration.bloodmagic.BloodMagicLPTransferAdapter;
import mekceuqiostorage.common.integration.bloodmagic.BloodMagicWillTransferAdapter;
import mekceuqiostorage.common.integration.botania.BotaniaManaTransferAdapter;
import mekceuqiostorage.common.integration.embers.EmbersEmberTransferAdapter;
import mekceuqiostorage.common.integration.naturesaura.NaturesAuraTransferAdapter;
import mekceuqiostorage.common.integration.pneumaticcraft.PneumaticCraftAirTransferAdapter;
import mekceuqiostorage.common.integration.thaumcraft.ThaumcraftEssentiaTransferAdapter;
import mekceuqiostorage.common.registration.QIOStorageRegistration;
import net.minecraftforge.fml.common.Optional;

/** Forge-gated entrypoints for common-side optional integrations. */
public final class QIOStorageIntegrations {

    private static boolean registered;

    private QIOStorageIntegrations() {
    }

    public static synchronized void registerCommon() {
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
        QIOStorageRegistration.registerTransferAdapter(ThaumcraftEssentiaTransferAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.BOTANIA)
    private static void registerBotania() {
        QIOStorageRegistration.registerTransferAdapter(BotaniaManaTransferAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.BLOODMAGIC)
    private static void registerBloodMagic() {
        QIOStorageRegistration.registerTransferAdapter(BloodMagicLPTransferAdapter.INSTANCE);
        QIOStorageRegistration.registerTransferAdapter(BloodMagicWillTransferAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.ASTRALSORCERY)
    private static void registerAstralSorcery() {
        AstralSorceryIntegration.register();
    }

    @Optional.Method(modid = QIOStorageHooks.EMBERS)
    private static void registerEmbers() {
        QIOStorageRegistration.registerTransferAdapter(EmbersEmberTransferAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.NATURESAURA)
    private static void registerNaturesAura() {
        QIOStorageRegistration.registerTransferAdapter(NaturesAuraTransferAdapter.INSTANCE);
    }

    @Optional.Method(modid = QIOStorageHooks.PNEUMATICCRAFT)
    private static void registerPneumaticCraft() {
        QIOStorageRegistration.registerTransferAdapter(PneumaticCraftAirTransferAdapter.INSTANCE);
    }
}
