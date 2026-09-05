package mekceuqiostorage.client.integration.bloodmagic;

import WayofTime.bloodmagic.block.BlockLifeEssence;
import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/** Renders one owner-specific Blood Magic Soul Network LP identity. */
@SideOnly(Side.CLIENT)
public final class BloodMagicLPRenderer extends AbstractQIOResourceRenderer<SoulNetworkLP> {

    public static final BloodMagicLPRenderer INSTANCE = new BloodMagicLPRenderer();

    private static final Gui SPRITE_RENDERER = new Gui();

    private BloodMagicLPRenderer() {
        super(QIOStorageResourceSpecs.BLOODMAGIC_LP);
    }

    @Override
    public void render(@Nonnull SoulNetworkLP resource, int x, int y) {
        requireResource(resource);
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite lifeEssence = minecraft.getTextureMapBlocks().getAtlasSprite(
              BlockLifeEssence.getLifeEssence().getStill().toString());
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.color(1, 1, 1, 1);
        SPRITE_RENDERER.drawTexturedModalRect(x, y, lifeEssence, 16, 16);
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull SoulNetworkLP resource) {
        requireResource(resource);
        return I18n.format("qio.mekceuqiostorage.resource.bloodmagic_lp",
              displayOwner(resource));
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull SoulNetworkLP resource) {
        requireResource(resource);
        return "bloodmagic:soul_network/" + resource.getOwnerId();
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull SoulNetworkLP resource) {
        requireResource(resource);
        return Arrays.asList(getDisplayName(resource),
              TextFormatting.DARK_GRAY + I18n.format(
                    "qio.mekceuqiostorage.tooltip.source",
                    I18n.format("qio.mekceuqiostorage.source.bloodmagic")),
              TextFormatting.DARK_GRAY + I18n.format(
                    "qio.mekceuqiostorage.tooltip.unit.bloodmagic_lp"));
    }

    @Nonnull
    private static String displayOwner(SoulNetworkLP resource) {
        return resource.getOwnerName() == null ?
              resource.getOwnerId().toString().substring(0, 8) : resource.getOwnerName();
    }

    private static void requireResource(SoulNetworkLP resource) {
        QIOStorageResourceSpecs.BLOODMAGIC_LP.getCodec().normalize(resource);
    }
}
