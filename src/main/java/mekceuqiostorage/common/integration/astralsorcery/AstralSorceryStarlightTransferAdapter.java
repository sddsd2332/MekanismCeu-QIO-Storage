package mekceuqiostorage.common.integration.astralsorcery;

import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase;
import hellfirepvp.astralsorcery.common.starlight.IStarlightReceiver;
import hellfirepvp.astralsorcery.common.starlight.transmission.IPrismTransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.ITransmissionReceiver;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import hellfirepvp.astralsorcery.common.tile.TileRitualPedestal;
import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.AbstractSingleResourceTransferAdapter;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Emits starlight to adjacent AS receivers. QIO pays for emission, including discarded light. */
public final class AstralSorceryStarlightTransferAdapter
      extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {

    public static final AstralSorceryStarlightTransferAdapter INSTANCE =
          new AstralSorceryStarlightTransferAdapter();

    private AstralSorceryStarlightTransferAdapter() {
        super(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT, QIOStorageResources.STARLIGHT);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        try {
            if (!(target instanceof IStarlightReceiver) || target.isInvalid()) {
                return false;
            }
            World world = target.getWorld();
            return world != null && !world.isRemote && world.isBlockLoaded(target.getPos()) &&
                  world.getTileEntity(target.getPos()) == target;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    protected long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        return 0;
    }

    @Override
    protected long extractResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        return 0;
    }

    @Override
    protected long insertResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        if (action.simulate()) {
            return amount;
        }
        IStarlightReceiver receiver = (IStarlightReceiver) target;
        // Rituals keep their working state in the registered node, not in a fresh endpoint.
        IPrismTransmissionNode node = receiver.getNode();
        ITransmissionReceiver endpoint = node instanceof ITransmissionReceiver ?
              (ITransmissionReceiver) node : receiver.provideEndpoint(target.getPos());
        if (endpoint == null || !target.getPos().equals(endpoint.getLocationPos())) {
            return 0;
        }
        endpoint.onStarlightReceive(target.getWorld(), true, getConstellation(target),
              AstralSorceryStarlightMath.toNetworkAmount(amount));
        markDirtySafely(target);
        // The void callback may discard light at capacity or while inactive. This still counts
        // as emission; only failure to invoke the endpoint leaves the QIO transfer incomplete.
        return amount;
    }

    @Nullable
    private static IWeakConstellation getConstellation(TileEntity target) {
        try {
            if (target instanceof TileAltar) {
                TileAltar altar = (TileAltar) target;
                TileAltar.AltarLevel level = altar.getAltarLevel();
                if (level == null || level.ordinal() < TileAltar.AltarLevel.TRAIT_CRAFT.ordinal()) {
                    return null;
                }
                ItemStack focus = altar.getFocusItem();
                if (!focus.isEmpty() && focus.getItem() instanceof ItemTunedCrystalBase) {
                    IConstellation constellation = altar.getFocusedConstellation();
                    return constellation instanceof IWeakConstellation ?
                          (IWeakConstellation) constellation : null;
                }
            } else if (target instanceof TileRitualPedestal) {
                return ((TileRitualPedestal) target).getRitualConstellation();
            }
        } catch (LinkageError | RuntimeException ignored) {
            // Missing or malformed crystal metadata selects generic light without blocking output.
        }
        return null;
    }
}
