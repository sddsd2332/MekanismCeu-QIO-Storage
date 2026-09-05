package mekceuqiostorage.client;

import mekceuqiostorage.common.QIOStorageCommonProxy;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.registration.QIOStorageItems;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only registration hooks. The class is referenced by name from {@code @SidedProxy}. */
@SideOnly(Side.CLIENT)
public class QIOStorageClientProxy extends QIOStorageCommonProxy {

    @Override
    public void registerClientHandlers() {
        QIOStorageClientIntegrations.register();
    }

    @Override
    public void registerItemRenders() {
        for (ItemQIOStorageDrive item : QIOStorageItems.getRegisteredItems()) {
            if (item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                      new ModelResourceLocation(getBaseModel(item), "inventory"));
            }
        }
    }

    private static ResourceLocation getBaseModel(ItemQIOStorageDrive item) {
        QIOStorageDriveTier tier = item.getStorageTier();
        String path = "qio_drive_" + tier.getPath();
        return new ResourceLocation("mekanism", path);
    }
}
