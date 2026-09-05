package mekceuqiostorage.client.integration.botania;

import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.botania.client.core.handler.MiscellaneousIcons;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Renders Botania mana with the same animated mana-water sprite used by its pool.
 */
@SideOnly(Side.CLIENT)
public final class BotaniaManaRenderer
        extends AbstractQIOResourceRenderer<QIOStorageResources.Scalar> {

    public static final BotaniaManaRenderer INSTANCE = new BotaniaManaRenderer();

    private static final Gui SPRITE_RENDERER = new Gui();

    private BotaniaManaRenderer() {
        super(QIOStorageResourceSpecs.BOTANIA_MANA);
    }

    @Override
    public void render(@Nonnull QIOStorageResources.Scalar resource, int x, int y) {
        requireMana(resource);
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite manaWater = MiscellaneousIcons.INSTANCE.manaWater;
        if (manaWater == null) {
            throw new IllegalStateException("Botania mana-water sprite has not been stitched");
        }
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.color(1, 1, 1, 1);
        SPRITE_RENDERER.drawTexturedModalRect(x, y, manaWater, 16, 16);
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull QIOStorageResources.Scalar resource) {
        requireMana(resource);
        return I18n.format("qio.mekceuqiostorage.resource.botania_mana");
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull QIOStorageResources.Scalar resource) {
        requireMana(resource);
        return "botania:mana";
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull QIOStorageResources.Scalar resource) {
        requireMana(resource);
        return Arrays.asList(getDisplayName(resource),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.source",
                        I18n.format("qio.mekceuqiostorage.source.botania")),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.unit.botania_mana"));
    }

    private static void requireMana(QIOStorageResources.Scalar resource) {
        if (!QIOStorageResourceSpecs.BOTANIA_MANA.getCodec().sameType(
                QIOStorageResources.MANA, resource)) {
            throw new IllegalArgumentException("Renderer received a non-mana QIO resource");
        }
    }
}
