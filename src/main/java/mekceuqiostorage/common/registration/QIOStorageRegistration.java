package mekceuqiostorage.common.registration;

import mekanism.api.qio.resource.QIOResourceCodecRegistry;
import mekanism.api.qio.resource.QIOResourceTransferAdapter;
import mekanism.api.qio.resource.QIOResourceTransferAdapterRegistry;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.integration.transfer.AbstractQIOResourceTransferAdapter;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Small idempotence/validation helpers shared by integration bootstrap code.
 */
public final class QIOStorageRegistration {

    private QIOStorageRegistration() {
    }

    public static void registerTransferAdapter(
            @Nonnull AbstractQIOResourceTransferAdapter<?> adapter) {
        Objects.requireNonNull(adapter, "transfer adapter");
        registerTransferAdapter(adapter.getResourceSpec(), adapter);
    }

    public static void registerTransferAdapter(@Nonnull QIOStorageResourceSpec<?> resourceSpec,
                                               @Nonnull QIOResourceTransferAdapter adapter) {
        Objects.requireNonNull(resourceSpec, "QIO resource specification");
        Objects.requireNonNull(adapter, "transfer adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.getCodecId(), "transfer codec id");
        if (!resourceSpec.getCodecId().equals(id)) {
            throw new IllegalArgumentException("Transfer adapter codec does not match resource " +
                    resourceSpec.getCodecId() + ": " + id);
        }
        if (!QIOStorageBootstrap.isResourceEnabled(resourceSpec)) {
            throw new IllegalStateException("Cannot register transfer adapter while optional mod is absent: " +
                    resourceSpec.getModId());
        }
        if (!QIOResourceCodecRegistry.INSTANCE.isRegistered(id)) {
            throw new IllegalStateException("Cannot register transfer adapter before codec " + id);
        }
        QIOResourceTransferAdapter existing = QIOResourceTransferAdapterRegistry.INSTANCE.get(id);
        if (existing == null) {
            QIOResourceTransferAdapterRegistry.INSTANCE.register(adapter);
        } else if (existing != adapter) {
            throw new IllegalStateException("A transfer adapter is already registered for " + id);
        }
    }
}
