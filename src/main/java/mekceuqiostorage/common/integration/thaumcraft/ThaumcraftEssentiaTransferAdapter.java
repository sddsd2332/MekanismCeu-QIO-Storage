package mekceuqiostorage.common.integration.thaumcraft;

import com.google.common.collect.MapMaker;
import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.integration.transfer.AbstractQIOResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
import mekceuqiostorage.common.integration.transfer.NativeTransferAccounting;
import mekceuqiostorage.common.integration.transfer.UncertainTransferException;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.devices.TileCondenser;
import thaumcraft.common.tiles.essentia.TileEssentiaInput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

/** Direct Thaumcraft essentia transport integration. One QIO type is used per Aspect tag. */
public final class ThaumcraftEssentiaTransferAdapter
      extends AbstractQIOResourceTransferAdapter<Essentia> {

    public static final ThaumcraftEssentiaTransferAdapter INSTANCE =
          new ThaumcraftEssentiaTransferAdapter();

    /**
     * The importer applies its filters after asking an adapter for candidates. Keep a cursor per
     * actual tile and face so a rejected first Aspect cannot starve later Aspects forever.
     * Weak keys also allow unloaded tile entities to disappear without retaining the world.
     */
    private static final Map<TileEntity, Map<EnumFacing, String>> CANDIDATE_CURSORS =
          new MapMaker().weakKeys().makeMap();

    private ThaumcraftEssentiaTransferAdapter() {
        super(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        try {
            if (!isLive(target) || targetFace == null) {
                return false;
            }
            IAspectSource source = getSource(target);
            if (source != null && source.isBlocked()) {
                return false;
            }
            IEssentiaTransport transport = getTransport(target);
            if (transport != null) {
                if (isUnsupportedInput(target, transport, targetFace)) {
                    return false;
                }
                return transport.isConnectable(targetFace) &&
                      (transport.canInputFrom(targetFace) || transport.canOutputTo(targetFace));
            }
            return source != null;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    @Nonnull
    public List<QIOResourceStack> getExtractable(@Nonnull TileEntity target,
          @Nonnull EnumFacing targetFace, int maximumTypes, long maximumAmount) {
        try {
            IEssentiaTransport transport = getTransport(target);
            if (maximumTypes <= 0 || maximumAmount <= 0 || targetFace == null || !isLive(target)) {
                return Collections.emptyList();
            }
            IAspectSource source = getSource(target);
            if (source != null && source.isBlocked()) {
                return Collections.emptyList();
            }
            if (transport != null) {
                clearCursor(target, targetFace);
                if (!transport.isConnectable(targetFace) || !transport.canOutputTo(targetFace)) {
                    return Collections.emptyList();
                }
                Aspect aspect = transport.getEssentiaType(targetFace);
                int amount = transport.getEssentiaAmount(targetFace);
                if (!valid(aspect) || amount <= 0) {
                    return Collections.emptyList();
                }
                return candidate(Essentia.of(aspect.getTag()), amount, maximumTypes, maximumAmount);
            }
            if (source == null) {
                clearCursor(target, targetFace);
                return Collections.emptyList();
            }
            return discoverCandidates(target, targetFace, source, maximumTypes, maximumAmount);
        } catch (LinkageError | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    protected long extractResolved(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          @Nonnull Essentia resource, long amount, @Nonnull Action action) {
        try {
            IEssentiaTransport transport = getTransport(target);
            Aspect aspect = Aspect.getAspect(resource.getAspectTag());
            if (aspect == null || !valid(aspect) || targetFace == null) {
                return 0;
            }
            IAspectSource source = getSource(target);
            if (source != null && source.isBlocked()) {
                return 0;
            }
            if (transport != null) {
                if (transport instanceof TileEssentiaInput) {
                    return 0;
                }
                Aspect contained = transport.getEssentiaType(targetFace);
                if (!transport.isConnectable(targetFace) || !transport.canOutputTo(targetFace) ||
                      contained == null || !resource.getAspectTag().equals(contained.getTag())) {
                    return 0;
                }
                int requested = Math.min(QIOStorageTransferMath.intLimit(amount),
                      Math.max(0, transport.getEssentiaAmount(targetFace)));
                if (requested <= 0 || action.simulate()) {
                    return requested;
                }
                long moved = NativeTransferAccounting.reported(requested, false, null,
                      () -> transport.takeEssentia(aspect, requested, targetFace), null);
                moved = (int) QIOStorageTransferMath.result(moved, requested);
                if (moved > 0) {
                    markDirtySafely(target);
                }
                return moved;
            }
            if (source == null) {
                return 0;
            }
            int available = QIOStorageTransferMath.nonNegativeInt(source.containerContains(aspect));
            if (available <= 0 && source.doesContainerContainAmount(aspect, 1)) {
                // Mirrors expose their remote contents through the boolean probe only.
                available = 1;
            }
            int requested = Math.min(QIOStorageTransferMath.intLimit(amount), available);
            if (requested <= 0 || action.simulate()) {
                return requested;
            }
            long moved = NativeTransferAccounting.reported(requested, false, null,
                  () -> source.takeFromContainer(aspect, requested) ? requested : 0, null);
            if (moved > 0) {
                markDirtySafely(target);
            }
            return moved;
        } catch (UncertainTransferException failure) {
            throw failure;
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    protected long insertResolved(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          @Nonnull Essentia resource, long amount, @Nonnull Action action) {
        try {
            IEssentiaTransport transport = getTransport(target);
            Aspect aspect = Aspect.getAspect(resource.getAspectTag());
            if (aspect == null || !valid(aspect) || targetFace == null) {
                return 0;
            }
            IAspectSource source = getSource(target);
            if (source != null && source.isBlocked()) {
                return 0;
            }
            if (transport != null) {
                if (isUnsupportedInput(target, transport, targetFace)) {
                    return 0;
                }
                if (!transport.isConnectable(targetFace) ||
                      !transport.canInputFrom(targetFace) || !accepts(target, transport, targetFace, aspect)) {
                    return 0;
                }
                int requested = QIOStorageTransferMath.intLimit(amount);
                if (requested <= 0 || action.simulate()) {
                    // Thaumcraft has no non-mutating insert probe. Execution reports the native
                    // accepted amount and Mekanism restores any remainder to QIO.
                    return requested;
                }
                long moved = NativeTransferAccounting.reported(requested, true, null,
                      () -> transport.addEssentia(aspect, requested, targetFace), null);
                moved = (int) QIOStorageTransferMath.result(moved, requested);
                if (moved > 0) {
                    markDirtySafely(target);
                }
                return moved;
            }
            if (source == null || !source.doesContainerAccept(aspect)) {
                return 0;
            }
            // IAspectSource has no capacity query and the mirror implementation accepts one unit.
            int requested = Math.min(QIOStorageTransferMath.intLimit(amount), 1);
            if (requested <= 0 || action.simulate()) {
                return requested;
            }
            long moved = NativeTransferAccounting.reported(requested, true, null,
                  () -> requested - (long) source.addToContainer(aspect, requested), null);
            if (moved > 0) {
                markDirtySafely(target);
            }
            return moved;
        } catch (UncertainTransferException failure) {
            throw failure;
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean accepts(TileEntity target, IEssentiaTransport transport,
          EnumFacing targetFace, Aspect aspect) {
        if (target instanceof IAspectSource && ((IAspectSource) target).isBlocked()) {
            return false;
        }
        // Container implementations can report an output type on their input face (the
        // centrifuge does this), so their native add operation remains authoritative. A
        // non-container transport's non-null type is the only type it can accept.
        Aspect contained = transport.getEssentiaType(targetFace);
        if (!(target instanceof IAspectContainer) && contained != null &&
              !aspect.getTag().equals(contained.getTag())) {
            return false;
        }
        return !(target instanceof IAspectContainer) ||
              ((IAspectContainer) target).doesContainerAccept(aspect);
    }

    private static boolean valid(Aspect aspect) {
        return aspect != null && aspect.getTag() != null &&
              Aspect.getAspect(aspect.getTag()) == aspect;
    }

    private List<QIOResourceStack> discoverCandidates(TileEntity target, EnumFacing face,
          IAspectSource source, int maximumTypes, long maximumAmount) {
        Map<String, AvailableAspect> available = new LinkedHashMap<>();
        AspectList aspects = source.getAspects();
        if (aspects != null) {
            for (Aspect aspect : aspects.getAspectsSortedByName()) {
                if (!valid(aspect)) {
                    continue;
                }
                long amount = QIOStorageTransferMath.nonNegativeInt(aspects.getAmount(aspect));
                if (amount > 0) {
                    available.put(aspect.getTag(), new AvailableAspect(aspect, amount));
                }
            }
        }
        // Mirrors expose remote contents through the boolean probe rather than their local list.
        // Merge the catalog even when the local list is non-empty; otherwise a visible local
        // Aspect could hide a remote one indefinitely.
        for (Aspect aspect : Aspect.aspects.values()) {
            if (!valid(aspect) || available.containsKey(aspect.getTag())) {
                continue;
            }
            try {
                if (source.doesContainerContainAmount(aspect, 1)) {
                    long amount = QIOStorageTransferMath.nonNegativeInt(source.containerContains(aspect));
                    available.put(aspect.getTag(), new AvailableAspect(aspect, Math.max(1, amount)));
                }
            } catch (LinkageError | RuntimeException ignored) {
                // A broken remote probe must not hide other registered Aspects.
            }
        }
        if (available.isEmpty()) {
            clearCursor(target, face);
            return Collections.emptyList();
        }
        List<AvailableAspect> ordered = new ArrayList<>(available.values());
        ordered.sort(Comparator.comparing(entry -> entry.aspect.getTag()));
        String cursor = getCursor(target, face);
        int start = 0;
        if (cursor != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).aspect.getTag().compareTo(cursor) > 0) {
                    start = i;
                    break;
                }
            }
        }
        int limit = Math.min(maximumTypes, ordered.size());
        if (maximumAmount < limit) {
            limit = (int) maximumAmount;
        }
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<AvailableAspect> selected = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            selected.add(ordered.get((start + i) % ordered.size()));
        }
        setCursor(target, face, selected.get(selected.size() - 1).aspect.getTag());

        List<QIOResourceStack> result = new ArrayList<>(selected.size());
        long remaining = maximumAmount;
        int slots = selected.size();
        for (AvailableAspect entry : selected) {
            long fairShare = Math.max(1, remaining / slots);
            long amount = Math.min(entry.amount, fairShare);
            if (amount > 0) {
                result.add(new QIOResourceStack(descriptor(Essentia.of(entry.aspect.getTag())), amount));
                remaining -= amount;
            }
            slots--;
            if (remaining <= 0) {
                break;
            }
        }
        return result;
    }

    private static String getCursor(TileEntity target, EnumFacing face) {
        synchronized (CANDIDATE_CURSORS) {
            Map<EnumFacing, String> byFace = CANDIDATE_CURSORS.get(target);
            return byFace == null ? null : byFace.get(face);
        }
    }

    private static boolean isLive(TileEntity target) {
        if (target == null || target.isInvalid() || target.getWorld() != null &&
              (target.getWorld().isRemote || !target.getWorld().isBlockLoaded(target.getPos()) ||
                    target.getWorld().getTileEntity(target.getPos()) != target)) {
            synchronized (CANDIDATE_CURSORS) {
                CANDIDATE_CURSORS.remove(target);
            }
            return false;
        }
        return true;
    }

    public static void forget(TileEntity target) {
        synchronized (CANDIDATE_CURSORS) {
            CANDIDATE_CURSORS.remove(target);
        }
    }

    public static void forgetWorld(net.minecraft.world.World world) {
        synchronized (CANDIDATE_CURSORS) {
            CANDIDATE_CURSORS.keySet().removeIf(tile -> tile.getWorld() == world);
        }
    }

    private static void setCursor(TileEntity target, EnumFacing face, String aspectTag) {
        synchronized (CANDIDATE_CURSORS) {
            Map<EnumFacing, String> byFace = CANDIDATE_CURSORS.get(target);
            if (byFace == null) {
                byFace = new java.util.EnumMap<>(EnumFacing.class);
                CANDIDATE_CURSORS.put(target, byFace);
            }
            byFace.put(face, aspectTag);
        }
    }

    private static void clearCursor(TileEntity target, EnumFacing face) {
        synchronized (CANDIDATE_CURSORS) {
            Map<EnumFacing, String> byFace = CANDIDATE_CURSORS.get(target);
            if (byFace != null) {
                byFace.remove(face);
                if (byFace.isEmpty()) {
                    CANDIDATE_CURSORS.remove(target);
                }
            }
        }
    }

    private static final class AvailableAspect {
        private final Aspect aspect;
        private final long amount;

        private AvailableAspect(Aspect aspect, long amount) {
            this.aspect = aspect;
            this.amount = amount;
        }
    }

    private static IEssentiaTransport getTransport(TileEntity target) {
        return target instanceof IEssentiaTransport ? (IEssentiaTransport) target : null;
    }

    private static boolean isUnsupportedInput(TileEntity target, IEssentiaTransport transport,
          EnumFacing targetFace) {
        // These two blocks expose an input-shaped transport face but do not retain an inserted
        // amount: they pull from the network themselves (input) or only condense their own flux.
        return transport instanceof TileEssentiaInput ||
              target instanceof TileCondenser && transport.canInputFrom(targetFace);
    }

    @Nullable
    private static IAspectSource getSource(TileEntity target) {
        return target instanceof IAspectSource ? (IAspectSource) target : null;
    }
}
