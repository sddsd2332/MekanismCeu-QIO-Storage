package mekceuqiostorage.client;

import mekanism.api.qio.client.QIOResourceRenderer;
import mekanism.api.qio.client.QIOResourceRendererRegistry;
import mekanism.api.qio.client.QIOResourceSelectionAdapter;
import mekanism.api.qio.client.QIOResourceSelectionAdapterRegistry;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Duplicate-safe client registry helpers used by optional integration entrypoints.
 */
@SideOnly(Side.CLIENT)
public final class QIOStorageClientRegistration {

    private QIOStorageClientRegistration() {
    }

    public static <T> void registerRenderer(@Nonnull AbstractQIOResourceRenderer<T> renderer) {
        Objects.requireNonNull(renderer, "resource renderer");
        registerRenderer(renderer.getResourceSpec(), renderer);
    }

    public static <T> void registerRenderer(@Nonnull QIOStorageResourceSpec<T> resourceSpec,
                                            @Nonnull QIOResourceRenderer<T> renderer) {
        Objects.requireNonNull(resourceSpec, "QIO resource specification");
        Objects.requireNonNull(renderer, "resource renderer");
        ResourceLocation id = Objects.requireNonNull(renderer.getCodecId(), "renderer codec id");
        validateEnabled(resourceSpec, id, "renderer");
        QIOResourceRenderer<?> existing = QIOResourceRendererRegistry.INSTANCE.get(id);
        if (existing == null) {
            QIOResourceRendererRegistry.INSTANCE.register(renderer);
        } else if (existing != renderer) {
            throw new IllegalStateException("A QIO renderer is already registered for " + id);
        }
    }

    public static void registerSelectionAdapter(
            @Nonnull AbstractQIOResourceSelectionAdapter<?> adapter) {
        Objects.requireNonNull(adapter, "selection adapter");
        registerSelectionAdapter(adapter.getResourceSpec(), adapter);
    }

    public static void registerSelectionAdapter(@Nonnull QIOStorageResourceSpec<?> resourceSpec,
                                                @Nonnull QIOResourceSelectionAdapter adapter) {
        Objects.requireNonNull(resourceSpec, "QIO resource specification");
        Objects.requireNonNull(adapter, "selection adapter");
        ResourceLocation id = Objects.requireNonNull(adapter.getCodecId(), "selection codec id");
        validateEnabled(resourceSpec, id, "selection adapter");
        QIOResourceSelectionAdapter existing = QIOResourceSelectionAdapterRegistry.INSTANCE.get(id);
        if (existing == null) {
            QIOResourceSelectionAdapterRegistry.INSTANCE.register(adapter);
        } else if (existing != adapter) {
            throw new IllegalStateException("A QIO selection adapter is already registered for " + id);
        }
    }

    private static void validateEnabled(QIOStorageResourceSpec<?> resourceSpec,
                                        ResourceLocation actualCodecId, String component) {
        if (!resourceSpec.getCodecId().equals(actualCodecId)) {
            throw new IllegalArgumentException("QIO " + component + " codec does not match resource " +
                    resourceSpec.getCodecId() + ": " + actualCodecId);
        }
        if (!QIOStorageBootstrap.isResourceEnabled(resourceSpec)) {
            throw new IllegalStateException("Cannot register QIO " + component +
                    " while optional mod is absent: " + resourceSpec.getModId());
        }
    }
}
