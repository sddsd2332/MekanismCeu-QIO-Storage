package mekceuqiostorage.client.integration.naturesaura;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.container.IAuraContainer;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/** Resolves Nature's Aura item containers and addon Aura drives. */
@SideOnly(Side.CLIENT)
public final class NaturesAuraSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<QIOStorageResources.Scalar> {

    public static final NaturesAuraSelectionAdapter INSTANCE = new NaturesAuraSelectionAdapter();

    private NaturesAuraSelectionAdapter() {
        super(QIOStorageResourceSpecs.NATURESAURA_AURA);
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        if (container.isEmpty()) {
            return Collections.emptyList();
        }
        if (isResourceDrive(container)) {
            return Collections.singletonList(descriptor(QIOStorageResources.AURA));
        }
        IAuraContainer capability;
        try {
            if (NaturesAuraAPI.capAuraContainer == null) {
                return Collections.emptyList();
            }
            if (!container.hasCapability(NaturesAuraAPI.capAuraContainer, null)) {
                return Collections.emptyList();
            }
            capability = container.getCapability(
                  NaturesAuraAPI.capAuraContainer, (EnumFacing) null);
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
        if (capability == null) {
            return Collections.emptyList();
        }
        try {
            int stored = capability.getStoredAura();
            int maximum = capability.getMaxAura();
            return stored > 0 && maximum > 0 && stored <= maximum ?
                  Collections.singletonList(descriptor(QIOStorageResources.AURA)) :
                  Collections.emptyList();
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }
}
