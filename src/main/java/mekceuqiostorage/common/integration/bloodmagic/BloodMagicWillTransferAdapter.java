package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.soul.EnumDemonWillType;
import WayofTime.bloodmagic.soul.IDemonWillConduit;
import WayofTime.bloodmagic.soul.IDemonWillGem;
import WayofTime.bloodmagic.tile.TileDemonCrucible;
import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.integration.transfer.AbstractQIOResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import mekceuqiostorage.common.integration.transfer.NativeTransferAccounting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Direct, simulated transfer of all five Will types through Blood Magic's native APIs. */
public final class BloodMagicWillTransferAdapter extends AbstractQIOResourceTransferAdapter<DemonWill> {

    public static final BloodMagicWillTransferAdapter INSTANCE = new BloodMagicWillTransferAdapter();
    private static final DemonWill[] TYPES = DemonWill.values();

    private BloodMagicWillTransferAdapter() {
        super(QIOStorageResourceSpecs.BLOODMAGIC_WILL);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        try {
            if (!(target instanceof IDemonWillConduit) || target.isInvalid()) {
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
    @Nonnull
    public List<QIOResourceStack> getExtractable(@Nonnull TileEntity target,
          @Nonnull EnumFacing targetFace, int maximumTypes, long maximumAmount) {
        if (maximumTypes <= 0 || maximumAmount <= 0 || !supports(target, targetFace)) {
            return Collections.emptyList();
        }
        List<QIOResourceStack> result = new ArrayList<>(Math.min(maximumTypes, TYPES.length));
        long remaining = maximumAmount;
        // The importer discovers at most four types per pass and filters them afterwards.
        // Rotate all five types so a filtered type can be discovered even while others stay full.
        int first = (int) Math.floorMod(target.getWorld().getTotalWorldTime(), TYPES.length);
        for (int offset = 0; offset < TYPES.length && result.size() < maximumTypes && remaining > 0; offset++) {
            DemonWill type = TYPES[(first + offset) % TYPES.length];
            long available = extract(target, targetFace, descriptor(type), remaining, Action.SIMULATE);
            if (available > 0) {
                result.add(new QIOResourceStack(descriptor(type), available));
                remaining -= available;
            }
        }
        return result;
    }

    @Override
    protected long extractResolved(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          @Nonnull DemonWill resource, long amount, @Nonnull Action action) {
        EnumDemonWillType type = EnumDemonWillType.valueOf(resource.name());
        long gemMoved = extractGem(target, type, amount, action);
        if (gemMoved > 0) {
            return gemMoved;
        }
        IDemonWillConduit conduit = (IDemonWillConduit) target;
        if (!conduit.canDrain(type)) {
            return 0;
        }
        long available = QIOStorageTransferMath.limit(amount,
              BloodMagicWillMath.toQIOUnits(conduit.getCurrentWill(type)));
        long requested = reportedUnits(conduit.drainDemonWill(type,
              BloodMagicWillMath.toWill(available), false), available);
        if (requested <= 0 || action.simulate()) {
            return requested;
        }
        long moved = NativeTransferAccounting.reported(requested, false, () -> conduit.getCurrentWill(type),
              () -> reportedUnits(conduit.drainDemonWill(type, BloodMagicWillMath.toWill(requested), true), requested),
              change -> compensate(conduit, type, change));
        if (moved > 0) {
            markDirtySafely(target);
        }
        return moved;
    }

    /**
     * A Demon Crucible keeps a Soul Gem in its inventory rather than in the conduit will map.
     * Read and drain that item through Blood Magic's public gem API before checking the map.
     */
    private long extractGem(@Nonnull TileEntity target, @Nonnull EnumDemonWillType type,
          long amount, @Nonnull Action action) {
        if (!(target instanceof TileDemonCrucible)) {
            return 0;
        }
        ItemStack stack = ((TileDemonCrucible) target).getStackInSlot(0);
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IDemonWillGem)) {
            return 0;
        }
        IDemonWillGem gem = (IDemonWillGem) stack.getItem();
        // Some Blood Magic gem getters initialize an absent tag. Keep simulation read-only by
        // giving those getters a copy and reserve the actual stack for EXECUTE.
        ItemStack inspected = action.simulate() ? stack.copy() : stack;
        long available = BloodMagicWillMath.toQIOUnits(gem.getWill(type, inspected));
        long requested = QIOStorageTransferMath.limit(amount, available);
        if (requested <= 0) {
            return 0;
        }
        long reported = action.simulate() ? reportedUnits(gem.drainWill(type, inspected,
              BloodMagicWillMath.toWill(requested), false), requested) :
              NativeTransferAccounting.reported(requested, false, () -> gem.getWill(type, inspected),
                    () -> reportedUnits(gem.drainWill(type, inspected, BloodMagicWillMath.toWill(requested), true), requested), null);
        if (reported > 0 && action.execute()) {
            markDirtySafely(target);
        }
        return reported;
    }

    @Override
    protected long insertResolved(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          @Nonnull DemonWill resource, long amount, @Nonnull Action action) {
        IDemonWillConduit conduit = (IDemonWillConduit) target;
        EnumDemonWillType type = EnumDemonWillType.valueOf(resource.name());
        if (!conduit.canFill(type)) {
            return 0;
        }
        long requested = reportedUnits(conduit.fillDemonWill(type,
              BloodMagicWillMath.toWill(amount), false), amount);
        if (requested <= 0 || action.simulate()) {
            return requested;
        }
        // DemonWillHolder.addWill(type, amount, max) reports the cap but adds the original
        // amount. Bounding EXECUTE with a fresh simulation also protects direct adapter callers.
        long moved = NativeTransferAccounting.reported(requested, true, () -> conduit.getCurrentWill(type),
              () -> reportedUnits(conduit.fillDemonWill(type, BloodMagicWillMath.toWill(requested), true), requested),
              change -> compensate(conduit, type, change));
        if (moved > 0) {
            markDirtySafely(target);
        }
        return moved;
    }

    private static long reportedUnits(double will, long requested) {
        return QIOStorageTransferMath.result(BloodMagicWillMath.toQIOUnits(will), requested);
    }

    private static void compensate(IDemonWillConduit conduit, EnumDemonWillType type, double change) {
        if (change > 0) conduit.fillDemonWill(type, change, true);
        else if (change < 0) conduit.drainDemonWill(type, -change, true);
    }
}
