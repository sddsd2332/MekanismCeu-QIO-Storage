package mekceuqiostorage.client.integration.thaumcraft;

import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resolves Aspect ingredients and the current contents of Thaumcraft essentia items. */
@SideOnly(Side.CLIENT)
public final class ThaumcraftEssentiaSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<Essentia> {

    public static final ThaumcraftEssentiaSelectionAdapter INSTANCE =
          new ThaumcraftEssentiaSelectionAdapter();

    private ThaumcraftEssentiaSelectionAdapter() {
        super(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA);
    }

    @Override
    @Nullable
    public QIOResourceDescriptor fromIngredient(@Nonnull Object ingredient) {
        try {
            if (!(ingredient instanceof Aspect)) {
                return null;
            }
            Aspect aspect = (Aspect) ingredient;
            return valid(aspect) ? descriptor(Essentia.of(aspect.getTag())) : null;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        try {
            if (container.isEmpty()) {
                return Collections.emptyList();
            }
            if (isResourceDrive(container)) {
                // A multi-Aspect drive must stay ambiguous. The bounded hint list is synchronized
                // from the authoritative server record and is only used for client selection.
                ItemQIOStorageDrive drive = (ItemQIOStorageDrive) container.getItem();
                int driveTypes = drive.getDriveMetadata(container).getTypes();
                if (driveTypes <= 0) {
                    return Collections.emptyList();
                }
                List<QIOResourceDescriptor> result = new ArrayList<>();
                for (String tag : drive.getEssentiaTags(container)) {
                    Aspect aspect = Aspect.getAspect(tag);
                    if (valid(aspect)) {
                        result.add(descriptor(Essentia.of(aspect.getTag())));
                    }
                    if (result.size() >= 32) {
                        break;
                    }
                }
                // Never turn an incompletely synchronized multi-type drive into a unique
                // selection. One type must be represented by exactly one valid candidate.
                return driveTypes == 1 ? result.size() == 1 ? result : Collections.emptyList() :
                      result.size() > 1 ? result : Collections.emptyList();
            }
            if (!(container.getItem() instanceof IEssentiaContainerItem)) {
                return Collections.emptyList();
            }
            IEssentiaContainerItem item = (IEssentiaContainerItem) container.getItem();
            if (item.ignoreContainedAspects()) {
                return Collections.emptyList();
            }
            AspectList aspects = item.getAspects(container);
            if (aspects == null || aspects.size() == 0) {
                return Collections.emptyList();
            }
            List<QIOResourceDescriptor> result = new ArrayList<>(Math.min(32, aspects.size()));
            for (Aspect aspect : aspects.getAspectsSortedByName()) {
                if (result.size() >= 32) {
                    break;
                }
                if (valid(aspect) && aspects.getAmount(aspect) > 0) {
                    result.add(descriptor(Essentia.of(aspect.getTag())));
                }
            }
            return result;
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    private static boolean valid(@Nullable Aspect aspect) {
        try {
            return aspect != null && aspect.getTag() != null &&
                  Aspect.getAspect(aspect.getTag()) == aspect;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
