package mekceuqiostorage.client.integration.astralsorcery;

import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/** Renders starlight with Astral Sorcery's shifting star item. */
@SideOnly(Side.CLIENT)
public final class AstralSorceryStarlightRenderer
        extends AbstractQIOResourceRenderer<QIOStorageResources.Scalar> {

    public static final AstralSorceryStarlightRenderer INSTANCE =
            new AstralSorceryStarlightRenderer();

    private static final ResourceLocation SHIFTING_STAR_ID =
            new ResourceLocation("astralsorcery", "itemshiftingstar");

    private final QIOStorageResources.Scalar resource;
    private final String registryName;

    private AstralSorceryStarlightRenderer() {
        super(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT);
        resource = QIOStorageResources.STARLIGHT;
        registryName = getResourceSpec().getModId() + ':' + resource;
    }

    @Override
    public void render(@Nonnull QIOStorageResources.Scalar value, int x, int y) {
        requireResource(value);
        Item item = Item.REGISTRY.getObject(SHIFTING_STAR_ID);
        if (item != null) {
            Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(
                    new ItemStack(item), x, y);
        }
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return I18n.format("qio.mekceuqiostorage.resource.astralsorcery_starlight");
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return registryName;
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull QIOStorageResources.Scalar value) {
        requireResource(value);
        return Arrays.asList(getDisplayName(value),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.source",
                        I18n.format("qio.mekceuqiostorage.source.astralsorcery")),
                TextFormatting.DARK_GRAY + I18n.format(
                        "qio.mekceuqiostorage.tooltip.unit.astralsorcery_starlight"));
    }

    private void requireResource(QIOStorageResources.Scalar value) {
        if (!getResourceSpec().getCodec().sameType(resource, value)) {
            throw new IllegalArgumentException("Renderer received the wrong QIO resource type");
        }
    }
}
