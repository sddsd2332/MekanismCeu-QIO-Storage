package mekceuqiostorage.smoke;

import WayofTime.bloodmagic.core.RegistrarBloodMagic;
import WayofTime.bloodmagic.core.RegistrarBloodMagicItems;
import WayofTime.bloodmagic.core.data.Binding;
import mekanism.api.qio.client.QIOResourceRendererRegistry;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.client.gui.qio.QIOFilterGuiResource;
import mekanism.client.gui.qio.QIOResourceSelection;
import mekanism.common.content.qio.filter.QIOFilter;
import mekanism.common.content.qio.filter.QIOFilterResourceHelper;
import mekceuqiostorage.client.integration.bloodmagic.BloodMagicLPRenderer;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

import static mekceuqiostorage.smoke.QIOStorageSmokeMod.check;

@SideOnly(Side.CLIENT)
final class BloodMagicLPClientSmokeTest {

    private BloodMagicLPClientSmokeTest() {
    }

    static void verify() {
        UUID owner = UUID.fromString("10000000-0000-0000-0000-000000000001");
        ItemStack orb = new ItemStack(RegistrarBloodMagicItems.BLOOD_ORB);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("orb", RegistrarBloodMagic.ORB_WEAK.getRegistryName().toString());
        tag.setTag("binding", new Binding(owner, "Alice").serializeNBT());
        orb.setTagCompound(tag);
        NBTTagCompound before = orb.writeToNBT(new NBTTagCompound());
        QIOResourceSelection.Resolution selection = QIOResourceSelection.fromCarried(orb, 1);
        check(selection.isUnique(), "Native bound Blood Orb was not selected by the QIO GUI");
        QIOResourceDescriptor descriptor = selection.getDescriptor();
        SoulNetworkLP lp = descriptor.resolve(QIOStorageCodecs.BLOODMAGIC_LP);
        check(lp != null && owner.equals(lp.getOwnerId()), "Native Blood Orb selected the wrong LP owner");
        QIOFilter filter = QIOFilterResourceHelper.createFilter(descriptor, false);
        check(filter != null && filter.hasFilter(), "GUI could not create the LP filter");
        check(descriptor.equals(QIOFilterGuiResource.descriptor(filter)), "LP filter lost its descriptor");
        check(QIOResourceRendererRegistry.INSTANCE.get(descriptor.getCodecId()) == BloodMagicLPRenderer.INSTANCE,
              "LP renderer is missing");
        check(QIOFilterGuiResource.name(filter).contains("Alice"), "LP GUI name is unknown");
        check(before.equals(orb.writeToNBT(new NBTTagCompound())), "GUI selection mutated the Blood Orb");
        QIOResourceDescriptor restored = QIOResourceDescriptor.read(descriptor.write());
        check(restored.isResolved() && restored.equals(descriptor), "LP descriptor did not survive serialization");
        tag.removeTag("binding");
        check(!QIOResourceSelection.fromCarried(orb, 1).isUnique(), "Unbound Blood Orb selected a Soul Network");
    }
}
