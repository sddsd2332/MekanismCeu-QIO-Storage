package mekceuqiostorage.client.integration.astralsorcery;

import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/** Selects the generic Astral Sorcery Starlight drive resource. */
@SideOnly(Side.CLIENT)
public final class AstralSorceryStarlightSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<QIOStorageResources.Scalar> {

    public static final AstralSorceryStarlightSelectionAdapter INSTANCE =
          new AstralSorceryStarlightSelectionAdapter();

    private AstralSorceryStarlightSelectionAdapter() {
        super(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT);
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        return isResourceDrive(container) ?
              Collections.singletonList(descriptor(QIOStorageResources.STARLIGHT)) :
              Collections.emptyList();
    }
}
