package mekceuqiostorage.smoke;

import WayofTime.bloodmagic.core.RegistrarBloodMagicItems;
import WayofTime.bloodmagic.item.soul.ItemSoulGem;
import WayofTime.bloodmagic.soul.EnumDemonWillType;
import WayofTime.bloodmagic.soul.IDemonWill;
import mekanism.api.qio.client.QIOResourceRendererRegistry;
import mekanism.api.qio.client.QIOResourceSelectionAdapterRegistry;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicLPSelectionAdapter;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicWillRenderer;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicWillSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.registration.QIOStorageItems;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static mekceuqiostorage.smoke.QIOStorageSmokeMod.check;

@SideOnly(Side.CLIENT)
final class BloodMagicWillClientSmokeTest {

    private BloodMagicWillClientSmokeTest() {
    }

    static void verify() {
        BloodMagicLPClientSmokeTest.verify();
        BloodMagicWillSelectionAdapter adapter = BloodMagicWillSelectionAdapter.INSTANCE;
        check(QIOResourceSelectionAdapterRegistry.INSTANCE.get(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId()) == adapter,
              "Will selector not registered");
        check(QIOResourceRendererRegistry.INSTANCE.get(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId()) == BloodMagicWillRenderer.INSTANCE,
              "Will renderer not registered");
        ItemSoulGem gem = (ItemSoulGem) RegistrarBloodMagicItems.SOUL_GEM;
        List<QIOResourceDescriptor> expected = new ArrayList<>();
        NBTTagList hints = new NBTTagList();
        for (DemonWill type : DemonWill.values()) {
            EnumDemonWillType nativeType = EnumDemonWillType.valueOf(type.name());
            QIOResourceDescriptor descriptor = QIOStorageDescriptors.will(type);
            expected.add(descriptor);
            hints.appendTag(descriptor.write());
            check(adapter.fromIngredient(BloodMagicWillRenderer.INSTANCE.getIngredient(type)).equals(descriptor),
                  "Native Will crystal did not select " + type);
            ItemStack rawWill = ((IDemonWill) RegistrarBloodMagicItems.MONSTER_SOUL).createWill(nativeType.ordinal(), 1.23);
            check(descriptor.equals(adapter.fromIngredient(rawWill)), "Native Will item did not select " + type);
            ItemStack gemStack = new ItemStack(gem);
            gem.setWill(nativeType, gemStack, 0.29);
            NBTTagCompound before = gemStack.writeToNBT(new NBTTagCompound());
            check(descriptor.equals(adapter.fromIngredient(gemStack)), "Native gem did not select " + type);
            check(before.equals(gemStack.writeToNBT(new NBTTagCompound())), "Will selection mutated the gem");
            check(!BloodMagicWillRenderer.INSTANCE.getDisplayName(type).startsWith("qio."), "Missing Will display translation");
        }
        ItemStack emptyGem = new ItemStack(gem);
        NBTTagCompound before = emptyGem.writeToNBT(new NBTTagCompound());
        check(QIOStorageDescriptors.will(DemonWill.DEFAULT).equals(adapter.fromIngredient(emptyGem)),
              "Empty native gem did not select raw Will");
        check(before.equals(emptyGem.writeToNBT(new NBTTagCompound())), "Empty gem selection initialized its original NBT");

        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            ItemQIOStorageDrive will = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId(), tier.getDefinition());
            ItemQIOStorageDrive lp = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BLOODMAGIC_LP.getCodecId(), tier.getDefinition());
            check(will != null && lp != null && will != lp, "Independent LP/Will drive items missing");
            ItemStack willStack = new ItemStack(will);
            will.setDriveMetadata(willStack, 145, 5, 145);
            willStack.getTagCompound().setTag("qioWillResources", hints.copy());
            check(adapter.getContainedResources(willStack).equals(expected), "Will drive selections differ from its five hints");
            check(adapter.fromIngredient(willStack) == null, "Multi-type Will drive selected an arbitrary type");
            check(BloodMagicLPSelectionAdapter.INSTANCE.getContainedResources(willStack).isEmpty(), "LP selector claimed a Will disk");

            ItemStack lpStack = new ItemStack(lp);
            lp.setDriveMetadata(lpStack, 25, 1, 25);
            UUID owner = UUID.randomUUID();
            lpStack.getTagCompound().setUniqueId("qioSoulNetworkOwner", owner);
            lpStack.getTagCompound().setString("qioSoulNetworkOwnerName", "Alice");
            check(lp.getTypeCapacity(lpStack) == 1 && will.getTypeCapacity(willStack) == 5, "LP/Will type limits changed");
            check(BloodMagicLPSelectionAdapter.INSTANCE.getContainedResources(lpStack).equals(
                  Collections.singletonList(QIOStorageDescriptors.lp(owner))), "LP disk lost its owner selection");
            check(adapter.getContainedResources(lpStack).isEmpty(), "Will selector claimed an LP disk");
        }
        check(adapter.fromIngredient(ItemStack.EMPTY) == null && adapter.fromIngredient(new ItemStack(Items.STICK)) == null,
              "Unrelated items selected Will");
    }
}
