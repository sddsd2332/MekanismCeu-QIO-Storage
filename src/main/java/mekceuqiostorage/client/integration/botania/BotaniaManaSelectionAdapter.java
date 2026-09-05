package mekceuqiostorage.client.integration.botania;

import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.botania.api.mana.IManaItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Resolves charged Botania mana items and addon Mana drives. */
@SideOnly(Side.CLIENT)
public final class BotaniaManaSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<QIOStorageResources.Scalar> {

    public static final BotaniaManaSelectionAdapter INSTANCE = new BotaniaManaSelectionAdapter();

    private BotaniaManaSelectionAdapter() {
        super(QIOStorageResourceSpecs.BOTANIA_MANA);
    }

    /**
     * Botania does not expose a separate mana ingredient type. The API item interface itself is
     * nevertheless a useful neutral ingredient for callers that provide one (for example an
     * addon recipe viewer). Mekanism's normal selection path handles ItemStack ingredients before
     * consulting this method, so recognizing a charged ItemStack here does not change the left
     * click meaning of a carried stack.
     */
    @Override
    @Nullable
    public QIOResourceDescriptor fromIngredient(@Nonnull Object ingredient) {
        try {
            if (ingredient instanceof IManaItem) {
                return descriptor(QIOStorageResources.MANA);
            }
            if (ingredient instanceof ItemStack &&
                  !getContainedResources((ItemStack) ingredient).isEmpty()) {
                return descriptor(QIOStorageResources.MANA);
            }
        } catch (LinkageError | RuntimeException ignored) {
            // A third-party ingredient must never prevent the QIO ghost slot from opening.
        }
        return null;
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        if (container == null || container.isEmpty()) {
            return Collections.emptyList();
        }
        if (isResourceDrive(container)) {
            return Collections.singletonList(descriptor(QIOStorageResources.MANA));
        }
        if (!(container.getItem() instanceof IManaItem)) {
            return Collections.emptyList();
        }
        int mana;
        try {
            mana = ((IManaItem) container.getItem()).getMana(container);
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
        int maximum;
        try {
            maximum = ((IManaItem) container.getItem()).getMaxMana(container);
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
        if (mana <= 0 || maximum <= 0 || mana > maximum) {
            return Collections.emptyList();
        }
        return Collections.singletonList(descriptor(QIOStorageResources.MANA));
    }
}
