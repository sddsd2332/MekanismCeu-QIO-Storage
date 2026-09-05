package mekceuqiostorage.client;

import mekanism.api.qio.client.QIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Common type-safe renderer metadata; optional integrations provide the actual icon drawing.
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractQIOResourceRenderer<T> implements QIOResourceRenderer<T> {

    private final QIOStorageResourceSpec<T> resourceSpec;

    protected AbstractQIOResourceRenderer(@Nonnull QIOStorageResourceSpec<T> resourceSpec) {
        this.resourceSpec = Objects.requireNonNull(resourceSpec, "QIO resource specification");
    }

    @Nonnull
    public final QIOStorageResourceSpec<T> getResourceSpec() {
        return resourceSpec;
    }

    @Override
    @Nonnull
    public final ResourceLocation getCodecId() {
        return resourceSpec.getCodecId();
    }

    @Override
    @Nonnull
    public final Class<T> getValueClass() {
        return resourceSpec.getCodec().getValueClass();
    }

    @Override
    @Nonnull
    public String getModId(@Nonnull T resource) {
        return resourceSpec.getModId();
    }
}
