package mekceuqiostorage.client.integration.bloodmagic;

import WayofTime.bloodmagic.core.data.Binding;
import WayofTime.bloodmagic.iface.IBindable;
import WayofTime.bloodmagic.orb.IBloodOrb;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Resolves a bound Blood Orb or populated LP drive to its owner's Soul Network identity. */
@SideOnly(Side.CLIENT)
public final class BloodMagicLPSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<SoulNetworkLP> {

    public static final BloodMagicLPSelectionAdapter INSTANCE = new BloodMagicLPSelectionAdapter();

    private BloodMagicLPSelectionAdapter() {
        super(QIOStorageResourceSpecs.BLOODMAGIC_LP);
    }

    @Override
    @Nullable
    public QIOResourceDescriptor fromIngredient(@Nonnull Object ingredient) {
        if (!(ingredient instanceof ItemStack)) {
            return null;
        }
        List<QIOResourceDescriptor> contained = getContainedResources((ItemStack) ingredient);
        return contained.size() == 1 ? contained.get(0) : null;
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        if (container.isEmpty()) {
            return Collections.emptyList();
        }
        if (isResourceDrive(container)) {
            ItemQIOStorageDrive drive = (ItemQIOStorageDrive) container.getItem();
            SoulNetworkLP network = drive.getSingleSoulNetwork(container);
            return network == null ? Collections.emptyList() :
                  Collections.singletonList(descriptor(network));
        }
        try {
            if (!(container.getItem() instanceof IBloodOrb) ||
                  !(container.getItem() instanceof IBindable) ||
                  ((IBloodOrb) container.getItem()).getOrb(container) == null) {
                return Collections.emptyList();
            }
            Binding binding = ((IBindable) container.getItem()).getBinding(container);
            if (binding == null || binding.getOwnerId() == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(descriptor(SoulNetworkLP.of(
                  binding.getOwnerId(), binding.getOwnerName())));
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }
}
