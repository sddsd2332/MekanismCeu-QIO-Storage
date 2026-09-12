package mekceuqiostorage.common.integration.botania;

import mekanism.api.Action;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources;
import mekceuqiostorage.common.integration.transfer.AbstractSingleResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import mekceuqiostorage.common.integration.transfer.NativeTransferAccounting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PoolVariant;
import vazkii.botania.common.block.tile.mana.TilePool;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Direct Botania mana-pool and receiver integration. Mana has one canonical QIO identity. */
public final class BotaniaManaTransferAdapter
      extends AbstractSingleResourceTransferAdapter<QIOStorageResources.Scalar> {

    public static final BotaniaManaTransferAdapter INSTANCE = new BotaniaManaTransferAdapter();

    private BotaniaManaTransferAdapter() {
        super(QIOStorageResourceSpecs.BOTANIA_MANA, QIOStorageResources.MANA);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        // QIO transfer callbacks are authoritative server operations. A client-side probe must
        // never mutate a mirrored pool (or trigger Botania's visual side effects).
        if (target == null || target.isInvalid() ||
              target.getWorld() != null && target.getWorld().isRemote) {
            return false;
        }
        if (!(target instanceof IManaPool)) {
            return BotaniaManaReceiverEndpoint.supports(target);
        }
        IManaPool pool = (IManaPool) target;
        Boolean outputting = readOutputting(pool);
        // Output mode is the direction contract for pools. Receiver capability is checked only
        // on insertion, so a custom output-only pool is still a valid source.
        return outputting != null;
    }

    @Override
    protected long getStored(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        IManaPool pool = getPool(target);
        Boolean outputting = pool == null ? null : readOutputting(pool);
        Integer current = pool == null ? null : readCurrentMana(pool);
        if (pool == null || outputting == null || !outputting || current == null) {
            return 0;
        }
        if (isCreativePool(target, pool)) {
            return Integer.MAX_VALUE;
        }
        if (pool instanceof TilePool) {
            long capacity = directCapacity(target, (TilePool) pool);
            if (capacity <= 0 || current > capacity) {
                // A malformed or partially restored pool must not publish an impossible balance
                // to the QIO importer. It remains untouched so Botania can repair it normally.
                return 0;
            }
        }
        return current;
    }

    @Override
    protected long extractResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IManaPool pool = getPool(target);
        Boolean outputting = pool == null ? null : readOutputting(pool);
        Integer beforeValue = pool == null ? null : readCurrentMana(pool);
        if (pool == null || outputting == null || !outputting || beforeValue == null) {
            return 0;
        }
        int before = beforeValue;
        boolean creative = isCreativePool(target, pool);
        int requested = creative ? QIOStorageTransferMath.intLimit(amount) :
              Math.min(before, QIOStorageTransferMath.intLimit(amount));
        if (requested <= 0) {
            return requested;
        }
        // A creative pool advertises a constant one-million mana value even after receiving a
        // negative amount. It is an effectively infinite source, so do not mutate it or infer a
        // zero-sized transfer from the unchanged getter.
        if (creative) {
            return requested;
        }
        if (action.simulate()) {
            return requested;
        }
        long moved = NativeTransferAccounting.observed(requested, false, pool::getCurrentMana,
              () -> pool.recieveMana(-requested), change -> pool.recieveMana((int) change));
        if (moved > 0) {
            markDirtySafely(target);
        }
        return moved;
    }

    @Override
    protected long insertResource(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          long amount, @Nonnull Action action) {
        IManaPool pool = getPool(target);
        if (pool == null) {
            long inserted = BotaniaManaReceiverEndpoint.insert(target, amount, action);
            if (inserted > 0 && action.execute()) {
                markDirtySafely(target);
            }
            return inserted;
        }
        Boolean outputting = readOutputting(pool);
        Integer beforeValue = readCurrentMana(pool);
        if (outputting == null || outputting || beforeValue == null ||
              !canReceiveMana(pool)) {
            return 0;
        }
        if (isCreativePool(target, pool)) {
            return 0;
        }
        int before = beforeValue;
        // QIO writes through IManaPool.recieveMana, which is a direct storage operation. Do not
        // use TilePool#getAvailableSpaceForMana here: Botania deliberately reports virtual space
        // when a Mana Void is below a full pool so that mana bursts can be consumed by the void.
        // A direct recieveMana call does not perform that burst routing and would otherwise make
        // the exporter report mana that was never stored.
        long available = availableSpace(target, pool);
        int requested = QIOStorageTransferMath.intLimit(
              QIOStorageTransferMath.limit(amount, available));
        if (requested <= 0) {
            return requested;
        }
        if (action.simulate()) {
            return requested;
        }
        long moved = NativeTransferAccounting.observed(requested, true, pool::getCurrentMana,
              () -> pool.recieveMana(requested), change -> pool.recieveMana((int) change));
        if (moved > 0) {
            markDirtySafely(target);
        }
        return moved;
    }

    private static IManaPool getPool(TileEntity target) {
        return target instanceof IManaPool ? (IManaPool) target : null;
    }

    private static boolean canReceiveMana(IManaPool pool) {
        try {
            return pool.canRecieveManaFromBursts();
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @Nullable
    private static Integer readCurrentMana(IManaPool pool) {
        try {
            int current = pool.getCurrentMana();
            return current < 0 ? null : current;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean readOutputting(IManaPool pool) {
        try {
            return pool.isOutputtingPower();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static long availableSpace(TileEntity target, IManaPool pool) {
        Integer currentValue = readCurrentMana(pool);
        if (currentValue == null) {
            return 0;
        }
        long current = currentValue;
        if (pool instanceof TilePool) {
            try {
                TilePool tilePool = (TilePool) pool;
                // manaCap is initialized on the first server tick. During the small window before
                // that tick, use the block variant only as a conservative fallback. In particular,
                // never derive direct capacity from getAvailableSpaceForMana(), as that method
                // includes the virtual Mana Void overflow path described above.
                long capacity = directCapacity(target, tilePool);
                return capacity > current ? QIOStorageTransferMath.limit(
                      capacity - current, Integer.MAX_VALUE) : 0;
            } catch (LinkageError | RuntimeException ignored) {
                return 0;
            }
        }
        // IManaPool does not expose a portable capacity accessor. isFull() is the only safe
        // capability-level guard for third-party pools; the post-write delta remains authoritative
        // when an implementation accepts less than the optimistic request.
        try {
            if (pool.isFull()) {
                return 0;
            }
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
        return QIOStorageTransferMath.limit(Integer.MAX_VALUE - current, Integer.MAX_VALUE);
    }

    private static long directCapacity(TileEntity target, TilePool pool) {
        return pool.manaCap > 0 ? pool.manaCap : variantCapacity(target);
    }

    private static long variantCapacity(TileEntity target) {
        try {
            IBlockState state = target.getWorld() == null ? null :
                  target.getWorld().getBlockState(target.getPos());
            if (state != null && state.getPropertyKeys().contains(BotaniaStateProps.POOL_VARIANT)) {
                return state.getValue(BotaniaStateProps.POOL_VARIANT) == PoolVariant.DILUTED ?
                      TilePool.MAX_MANA_DILLUTED : TilePool.MAX_MANA;
            }
        } catch (LinkageError | RuntimeException ignored) {
            // Unknown pool state: insertion remains disabled rather than over-reporting space.
        }
        return 0;
    }

    private static boolean isCreativePool(TileEntity target, IManaPool pool) {
        if (!(pool instanceof TilePool)) {
            return false;
        }
        try {
            IBlockState state = target.getWorld() == null ? null :
                  target.getWorld().getBlockState(target.getPos());
            return state != null && state.getPropertyKeys().contains(BotaniaStateProps.POOL_VARIANT) &&
                  state.getValue(BotaniaStateProps.POOL_VARIANT) == PoolVariant.CREATIVE;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

}
