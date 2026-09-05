package mekceuqiostorage.common;

import mekceuqiostorage.common.integration.QIOStorageIntegrations;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;

/** Server-safe lifecycle hooks. Client-only integrations live in the sided proxy. */
public class QIOStorageCommonProxy {

    public void preInit() {
        QIOStorageBootstrap.initialize();
        QIOStorageIntegrations.registerCommon();
    }

    public void registerClientHandlers() {
    }

    public void registerItemRenders() {
    }
}
