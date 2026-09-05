package mekceuqiostorage.common.integration.astralsorcery;

import hellfirepvp.astralsorcery.common.starlight.network.StarlightNetworkRegistry;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.registration.QIOStorageRegistration;

/** Registers the native AS importer endpoint and adjacent exporter adapter after AS is present. */
public final class AstralSorceryIntegration {

    private static boolean registered;

    private AstralSorceryIntegration() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        StarlightNetworkRegistry.registerEndpoint(AstralSorceryStarlightEndpoint.INSTANCE);
        QIOStorageRegistration.registerTransferAdapter(AstralSorceryStarlightTransferAdapter.INSTANCE);
        registered = true;
        QIOStorage.LOGGER.info("Registered Astral Sorcery QIO input endpoint and adjacent Starlight transfer adapter");
    }
}
