package mekceuqiostorage.client.integration.bloodmagic;

import WayofTime.bloodmagic.soul.EnumDemonWillType;
import mekceuqiostorage.client.AbstractQIOResourceRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/** Uses Blood Magic's native crystal item rendering for each Will identity. */
@SideOnly(Side.CLIENT)
public final class BloodMagicWillRenderer extends AbstractQIOResourceRenderer<DemonWill> {

    public static final BloodMagicWillRenderer INSTANCE = new BloodMagicWillRenderer();

    private BloodMagicWillRenderer() {
        super(QIOStorageResourceSpecs.BLOODMAGIC_WILL);
    }

    @Override
    public void render(@Nonnull DemonWill resource, int x, int y) {
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(getIngredient(resource), x, y);
    }

    @Override
    @Nonnull
    public String getDisplayName(@Nonnull DemonWill resource) {
        return I18n.format("qio.mekceuqiostorage.resource.bloodmagic_will." + resource.getKey());
    }

    @Override
    @Nonnull
    public String getRegistryName(@Nonnull DemonWill resource) {
        return "bloodmagic:will/" + resource.getKey();
    }

    @Override
    @Nonnull
    public List<String> getTooltip(@Nonnull DemonWill resource) {
        return Arrays.asList(getDisplayName(resource),
              TextFormatting.DARK_GRAY + I18n.format("qio.mekceuqiostorage.tooltip.source",
                    I18n.format("qio.mekceuqiostorage.source.bloodmagic")),
              TextFormatting.DARK_GRAY + I18n.format("qio.mekceuqiostorage.tooltip.unit.bloodmagic_will"));
    }

    @Override
    @Nonnull
    public ItemStack getIngredient(@Nonnull DemonWill resource) {
        return EnumDemonWillType.valueOf(resource.name()).getStack(1);
    }
}
