package mekceuqiostorage.common.integration.astralsorcery;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.common.content.qio.QIOAmount;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.content.qio.QIOResourceEntry;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekanism.common.tile.qio.TileEntityQIOImporter;
import mekanism.common.util.MekanismUtils;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Direct QIO importer operations used by the AS network callbacks. */
public final class AstralSorceryQIOTransfer {

    private AstralSorceryQIOTransfer() {
    }

    public static long receive(@Nonnull TileEntityQIOImporter importer, double networkAmount) {
        long amount = AstralSorceryStarlightMath.toQIOUnits(networkAmount);
        if (amount <= 0 || !canFunction(importer)) {
            return 0;
        }
        QIOFrequency frequency = importer.getQIOFrequency();
        if (!isUsable(frequency)) {
            return 0;
        }
        QIOResourceDescriptor descriptor = QIOStorageDescriptors.starlight();
        if (!matches(importer, descriptor)) {
            return 0;
        }
        long accepted = frequency.massInsert(descriptor, amount, Action.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        return frequency.massInsert(descriptor, accepted, Action.EXECUTE);
    }

    private static boolean canFunction(@Nonnull TileEntity tile) {
        try {
            return tile.getWorld() != null && !tile.getWorld().isRemote &&
                  MekanismUtils.canFunction(tile);
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isUsable(@Nullable QIOFrequency frequency) {
        return frequency != null && frequency.isValid() && !frequency.isRemoved();
    }

    private static boolean matches(@Nonnull TileEntityQIOImporter importer, @Nonnull QIOResourceDescriptor descriptor) {
        try {
            java.util.UUID uuid = QIOResourceTypeRegistry.INSTANCE.getOrTrack(descriptor);
            QIOResourceEntry entry = QIOResourceEntry.of(uuid, descriptor, QIOAmount.of(1));
            return importer.matchesResource(entry);
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
