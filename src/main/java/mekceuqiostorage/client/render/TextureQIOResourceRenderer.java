package mekceuqiostorage.client.render;

import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Client renderer for a single canonical resource using a native mod texture. */
@SideOnly(Side.CLIENT)
public final class TextureQIOResourceRenderer<T> extends AbstractQIOResourceRenderer<T> {

    private final T resource;
    private final ResourceLocation texture;
    private final String displayNameKey;
    private final String sourceNameKey;
    private final String unitKey;
    private final String registryName;

    public TextureQIOResourceRenderer(@Nonnull QIOStorageResourceSpec<T> resourceSpec,
          @Nonnull T resource, @Nonnull ResourceLocation texture, @Nonnull String displayNameKey,
          @Nonnull String sourceNameKey, @Nonnull String unitKey) {
        super(resourceSpec);
        this.resource = resourceSpec.getCodec().normalize(
              Objects.requireNonNull(resource, "resource template"));
        this.texture = Objects.requireNonNull(texture, "resource texture");
        this.displayNameKey = Objects.requireNonNull(displayNameKey, "display name key");
        this.sourceNameKey = Objects.requireNonNull(sourceNameKey, "source name key");
        this.unitKey = Objects.requireNonNull(unitKey, "unit key");
        registryName = resourceSpec.getModId() + ':' + this.resource.toString();
    }

    @Override
    public void render(@Nonnull T value, int x, int y) {
        requireResource(value);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull T value) {
        requireResource(value);
        return I18n.format(displayNameKey);
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull T value) {
        requireResource(value);
        return registryName;
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull T value) {
        requireResource(value);
        return Arrays.asList(getDisplayName(value),
              TextFormatting.DARK_GRAY + I18n.format(
                    "qio.mekceuqiostorage.tooltip.source", I18n.format(sourceNameKey)),
              TextFormatting.DARK_GRAY + I18n.format(unitKey));
    }

    private void requireResource(T value) {
        if (!getResourceSpec().getCodec().sameType(resource,
              Objects.requireNonNull(value, "resource"))) {
            throw new IllegalArgumentException("Renderer received the wrong QIO resource type");
        }
    }
}
