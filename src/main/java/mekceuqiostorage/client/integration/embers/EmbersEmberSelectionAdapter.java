package mekceuqiostorage.client.integration.embers;

import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import teamroots.embers.api.capabilities.EmbersCapabilities;
import teamroots.embers.api.power.IEmberCapability;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/** Resolves Ember item capabilities and addon Ember drives. */
@SideOnly(Side.CLIENT)
public final class EmbersEmberSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<QIOStorageResources.Scalar> {

    public static final EmbersEmberSelectionAdapter INSTANCE = new EmbersEmberSelectionAdapter();

    private EmbersEmberSelectionAdapter() {
        super(QIOStorageResourceSpecs.EMBERS_EMBER);
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        if (container.isEmpty()) {
            return Collections.emptyList();
        }
        if (isResourceDrive(container)) {
            return Collections.singletonList(descriptor(QIOStorageResources.EMBER));
        }
        IEmberCapability capability;
        try {
            if (EmbersCapabilities.EMBER_CAPABILITY == null) {
                return Collections.emptyList();
            }
            if (!container.hasCapability(EmbersCapabilities.EMBER_CAPABILITY, null)) {
                return Collections.emptyList();
            }
            capability = container.getCapability(
                  EmbersCapabilities.EMBER_CAPABILITY, (EnumFacing) null);
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
        if (capability == null) {
            return Collections.emptyList();
        }
        try {
            double ember = capability.getEmber();
            double capacity = capability.getEmberCapacity();
            return QIOStorageTransferMath.isFinitePositive(ember) &&
                  QIOStorageTransferMath.isFinitePositive(capacity) && ember >= 1 &&
                  ember <= capacity ?
                  Collections.singletonList(descriptor(QIOStorageResources.EMBER)) :
                  Collections.emptyList();
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }
}
