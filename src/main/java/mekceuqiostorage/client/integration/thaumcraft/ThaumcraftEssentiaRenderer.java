package mekceuqiostorage.client.integration.thaumcraft;

import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/** Renders each stored essentia type with Thaumcraft's own Aspect glyph and color. */
@SideOnly(Side.CLIENT)
public final class ThaumcraftEssentiaRenderer extends AbstractQIOResourceRenderer<Essentia> {

    public static final ThaumcraftEssentiaRenderer INSTANCE = new ThaumcraftEssentiaRenderer();
    private static final ResourceLocation FALLBACK =
          new ResourceLocation("thaumcraft", "textures/misc/essentia.png");

    private ThaumcraftEssentiaRenderer() {
        super(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA);
    }

    @Override
    public void render(@Nonnull Essentia resource, int x, int y) {
        Aspect aspect = resolve(resource);
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(
                  aspect == null ? FALLBACK : aspect.getImage());
            if (aspect == null) {
                GlStateManager.color(1, 1, 1, 1);
            } else {
                int color = aspect.getColor();
                GlStateManager.color((color >> 16 & 255) / 255F,
                      (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            }
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        } catch (LinkageError | RuntimeException ignored) {
            // Optional client assets can be unavailable while Forge is constructing the GUI.
            Minecraft.getMinecraft().getTextureManager().bindTexture(FALLBACK);
            GlStateManager.color(1, 1, 1, 1);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        } finally {
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull Essentia resource) {
        Aspect aspect = resolve(resource);
        if (aspect == null) {
            return resource.getAspectTag();
        }
        try {
            String key = "tc.aspect." + aspect.getTag();
            return I18n.hasKey(key) ? I18n.format(key) : aspect.getName();
        } catch (LinkageError | RuntimeException ignored) {
            return resource.getAspectTag();
        }
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull Essentia resource) {
        return "thaumcraft:aspect/" + resource.getAspectTag();
    }

    @Override
    @Nullable
    public Object getIngredient(@Nonnull Essentia resource) {
        return resolve(resource);
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull Essentia resource) {
        return Arrays.asList(getDisplayName(resource),
              TextFormatting.DARK_GRAY + I18n.format("qio.mekceuqiostorage.tooltip.source",
                    I18n.format("qio.mekceuqiostorage.source.thaumcraft")),
              TextFormatting.DARK_GRAY + I18n.format(
                    "qio.mekceuqiostorage.tooltip.unit.thaumcraft_essentia"));
    }

    private static Aspect resolve(Essentia resource) {
        try {
            return Aspect.getAspect(resource.getAspectTag());
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }
}
