package mekceuqiostorage.smoke;

import mekanism.api.qio.client.QIOResourceRendererRegistry;
import mekanism.api.qio.client.QIOResourceSelectionAdapter;
import mekanism.api.qio.client.QIOResourceSelectionAdapterRegistry;
import mekanism.common.content.qio.QIODriveDefinition;
import mekceuqiostorage.client.integration.astralsorcery.AstralSorceryStarlightSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.registration.QIOStorageItems;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;

import static mekceuqiostorage.smoke.QIOStorageSmokeMod.check;

@SideOnly(Side.CLIENT)
final class QIOStorageClientSmokeTest {

    private QIOStorageClientSmokeTest() {
    }

    static void verify() {
        if ("lp".equals(System.getProperty("mekceuqiostorage.smoke.integration")) &&
              Loader.isModLoaded("bloodmagic")) {
            verifyLp();
            return;
        }
        if ("will".equals(System.getProperty("mekceuqiostorage.smoke.integration"))) {
            ResourceLocation willId = QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId();
            if (!Loader.isModLoaded("bloodmagic")) {
                check(QIOResourceSelectionAdapterRegistry.INSTANCE.get(willId) == null &&
                      QIOResourceRendererRegistry.INSTANCE.get(willId) == null,
                      "Will client integration registered without Blood Magic");
            } else {
                verifyWill();
            }
            return;
        }
        ResourceLocation id = QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT.getCodecId();
        QIOResourceSelectionAdapter adapter = QIOResourceSelectionAdapterRegistry.INSTANCE.get(id);
        if (!Loader.isModLoaded("astralsorcery")) {
            check(adapter == null && QIOResourceRendererRegistry.INSTANCE.get(id) == null,
                  "AS client integration registered without AS");
            return;
        }
        check(adapter == AstralSorceryStarlightSelectionAdapter.INSTANCE, "AS selector was not registered");
        check(QIOResourceRendererRegistry.INSTANCE.get(id) != null, "AS resource renderer was not registered");
        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            ItemQIOStorageDrive drive = QIOStorageItems.getDrive(id, tier.getDefinition());
            check(drive != null, "Missing AS drive tier " + tier);
            check(adapter.getContainedResources(new ItemStack(drive)).equals(
                  Collections.singletonList(QIOStorageDescriptors.starlight())), "AS drive did not select generic starlight");
        }
        check(adapter.getContainedResources(ItemStack.EMPTY).isEmpty(), "Empty stack selected AS starlight");
        check(adapter.getContainedResources(new ItemStack(Items.STICK)).isEmpty(), "Unrelated item selected AS starlight");
        ItemQIOStorageDrive mana = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BOTANIA_MANA.getCodecId(),
              QIODriveDefinition.BASE);
        if (mana != null) {
            check(adapter.getContainedResources(new ItemStack(mana)).isEmpty(), "Foreign drive selected AS starlight");
        }
    }

    @Optional.Method(modid = "bloodmagic")
    private static void verifyWill() {
        BloodMagicWillClientSmokeTest.verify();
    }

    @Optional.Method(modid = "bloodmagic")
    private static void verifyLp() {
        BloodMagicLPClientSmokeTest.verify();
    }
}
