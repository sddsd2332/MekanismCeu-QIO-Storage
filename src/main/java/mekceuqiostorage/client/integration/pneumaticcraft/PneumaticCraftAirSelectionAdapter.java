package mekceuqiostorage.client.integration.pneumaticcraft;

import mekanism.api.qio.resource.QIOResourceDescriptor;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import mekceuqiostorage.client.AbstractQIOResourceSelectionAdapter;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/** Resolves pressurizable items using air = pressure times volume. */
@SideOnly(Side.CLIENT)
public final class PneumaticCraftAirSelectionAdapter
      extends AbstractQIOResourceSelectionAdapter<QIOStorageResources.Scalar> {

    public static final PneumaticCraftAirSelectionAdapter INSTANCE =
          new PneumaticCraftAirSelectionAdapter();

    private PneumaticCraftAirSelectionAdapter() {
        super(QIOStorageResourceSpecs.PNEUMATICCRAFT_AIR);
    }

    @Override
    @Nonnull
    public List<QIOResourceDescriptor> getContainedResources(@Nonnull ItemStack container) {
        if (container.isEmpty()) {
            return Collections.emptyList();
        }
        if (isResourceDrive(container)) {
            return Collections.singletonList(descriptor(QIOStorageResources.AIR));
        }
        IPressurizable pressurizable;
        try {
            pressurizable = IPressurizable.of(container);
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
        if (pressurizable == null) {
            return Collections.emptyList();
        }
        try {
            int volume = pressurizable.getVolume(container);
            float pressure = pressurizable.getPressure(container);
            float maxPressure = pressurizable.maxPressure(container);
            double air = (double) pressure * (double) volume;
            double maxAir = (double) maxPressure * (double) volume;
            if (volume <= 0 || pressure < 0 || !Float.isFinite(pressure) ||
                  !QIOStorageTransferMath.isFinitePositive(maxPressure) ||
                  !QIOStorageTransferMath.isFinitePositive(air) || air < 1 ||
                  !QIOStorageTransferMath.isFinitePositive(maxAir) || air > maxAir) {
                return Collections.emptyList();
            }
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
        return Collections.singletonList(descriptor(QIOStorageResources.AIR));
    }
}
