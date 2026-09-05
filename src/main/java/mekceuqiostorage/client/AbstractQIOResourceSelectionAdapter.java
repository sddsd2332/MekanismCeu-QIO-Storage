package mekceuqiostorage.client;

import mekanism.api.qio.client.QIOResourceSelectionAdapter;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpec;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Type-safe descriptor creation shared by optional client selection adapters.
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractQIOResourceSelectionAdapter<T> implements QIOResourceSelectionAdapter {

    private final QIOStorageResourceSpec<T> resourceSpec;

    protected AbstractQIOResourceSelectionAdapter(
            @Nonnull QIOStorageResourceSpec<T> resourceSpec) {
        this.resourceSpec = Objects.requireNonNull(resourceSpec, "QIO resource specification");
    }

    @Nonnull
    public final QIOStorageResourceSpec<T> getResourceSpec() {
        return resourceSpec;
    }

    @Override
    @Nonnull
    public final ResourceLocation getCodecId() {
        return resourceSpec.getCodecId();
    }

    @Nonnull
    protected final QIOResourceDescriptor descriptor(@Nonnull T value) {
        return QIOResourceDescriptor.of(resourceSpec.getCodec(), value);
    }

    protected final boolean isResourceDrive(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemQIOStorageDrive &&
              ((ItemQIOStorageDrive) stack.getItem()).getResourceSpec() == resourceSpec;
    }
}
