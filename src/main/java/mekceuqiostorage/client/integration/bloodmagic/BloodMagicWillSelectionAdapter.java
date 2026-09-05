package mekceuqiostorage.client.integration.bloodmagic;

import WayofTime.bloodmagic.item.soul.ItemSoulGem;
import WayofTime.bloodmagic.soul.EnumDemonWillType;
import WayofTime.bloodmagic.soul.IDemonWill;
import WayofTime.bloodmagic.soul.IDemonWillGem;
import WayofTime.bloodmagic.soul.IDiscreteDemonWill;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selects Will identities from native Will items, gems, and dedicated Will drives. */
@SideOnly(Side.CLIENT)
public final class BloodMagicWillSelectionAdapter extends AbstractQIOResourceSelectionAdapter<DemonWill> {

    public static final BloodMagicWillSelectionAdapter INSTANCE = new BloodMagicWillSelectionAdapter();

    private BloodMagicWillSelectionAdapter() {
        super(QIOStorageResourceSpecs.BLOODMAGIC_WILL);
    }

    @Override
    @Nullable
    public QIOResourceDescriptor fromIngredient(@Nonnull Object ingredient) {
        if (!(ingredient instanceof ItemStack)) {
            return null;
        }
        List<QIOResourceDescriptor> resources = getContainedResources((ItemStack) ingredient);
        return resources.size() == 1 ? resources.get(0) : null;
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        if (container.isEmpty()) {
            return Collections.emptyList();
        }
        if (isResourceDrive(container)) {
            return ((ItemQIOStorageDrive) container.getItem()).getWillResources(container);
        }
        try {
            // Native gem getters may initialize NBT. Selection must not mutate the held item.
            ItemStack inspected = container.copy();
            Item item = inspected.getItem();
            if (item instanceof IDemonWill) {
                return selection(((IDemonWill) item).getType(inspected));
            }
            if (item instanceof IDiscreteDemonWill) {
                return selection(((IDiscreteDemonWill) item).getType(inspected));
            }
            if (item instanceof ItemSoulGem) {
                return selection(((ItemSoulGem) item).getCurrentType(inspected));
            }
            if (item instanceof IDemonWillGem) {
                List<QIOResourceDescriptor> resources = new ArrayList<>();
                for (EnumDemonWillType type : EnumDemonWillType.values()) {
                    double amount = ((IDemonWillGem) item).getWill(type, inspected);
                    if (Double.isFinite(amount) && amount > 0) {
                        resources.addAll(selection(type));
                    }
                }
                return resources;
            }
        } catch (LinkageError | RuntimeException ignored) {
            // Broken optional item metadata does not provide a resource selection.
        }
        return Collections.emptyList();
    }

    private List<QIOResourceDescriptor> selection(EnumDemonWillType type) {
        return type == null ? Collections.emptyList() :
              Collections.singletonList(descriptor(DemonWill.fromKey(type.name)));
    }
}
