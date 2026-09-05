package mekceuqiostorage.common.integration.thaumcraft;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.Essentia;
import mekceuqiostorage.common.integration.transfer.AbstractQIOResourceTransferAdapter;
import mekceuqiostorage.common.integration.transfer.QIOStorageTransferMath;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Direct Thaumcraft essentia transport integration. One QIO type is used per Aspect tag. */
public final class ThaumcraftEssentiaTransferAdapter
      extends AbstractQIOResourceTransferAdapter<Essentia> {

    public static final ThaumcraftEssentiaTransferAdapter INSTANCE =
          new ThaumcraftEssentiaTransferAdapter();

    private ThaumcraftEssentiaTransferAdapter() {
        super(QIOStorageResourceSpecs.THAUMCRAFT_ESSENTIA);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        try {
            if (targetFace == null) {
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
            if (maximumTypes <= 0 || maximumAmount <= 0 || targetFace == null) {
                return Collections.emptyList();
            }
            IAspectSource source = getSource(target);
            if (source != null && source.isBlocked()) {
                return Collections.emptyList();
            }
            if (transport != null) {
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
                return Collections.emptyList();
            }
            AspectList aspects = source.getAspects();
            List<QIOResourceStack> result = new ArrayList<>(Math.min(32, maximumTypes));
            long remaining = maximumAmount;
            Set<String> seen = new HashSet<>();
            if (aspects != null) {
                for (Aspect aspect : aspects.getAspectsSortedByName()) {
                    if (result.size() >= maximumTypes || result.size() >= 32 || remaining <= 0) {
                        break;
                    }
                    if (!valid(aspect) || !seen.add(aspect.getTag())) {
                        continue;
                    }
                    long available = QIOStorageTransferMath.nonNegativeInt(aspects.getAmount(aspect));
                    long bounded = Math.min(available, remaining);
                    if (bounded > 0) {
                        result.add(new QIOResourceStack(descriptor(Essentia.of(aspect.getTag())), bounded));
                        remaining -= bounded;
                    }
                }
            }
            if (result.isEmpty() && remaining > 0) {
                // Mirror sources do not expose their remote AspectList. Their boolean probe is
                // read-only, so use the registered Aspect catalog to discover candidates.
                for (Aspect aspect : Aspect.aspects.values()) {
                    if (result.size() >= maximumTypes || result.size() >= 32 || remaining <= 0) {
                        break;
                    }
                    if (!valid(aspect) || !seen.add(aspect.getTag())) {
                        continue;
                    }
                    try {
                        if (source.doesContainerContainAmount(aspect, 1)) {
                            result.add(new QIOResourceStack(
                                  descriptor(Essentia.of(aspect.getTag())), 1));
                            remaining--;
                        }
                    } catch (LinkageError | RuntimeException ignored) {
                        // A broken remote source must not hide other registered Aspects.
                    }
                }
            }
            return result;
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
                int moved = transport.takeEssentia(aspect, requested, targetFace);
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
            int moved = source.takeFromContainer(aspect, requested) ? requested : 0;
            if (moved > 0) {
                markDirtySafely(target);
            }
            return moved;
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
                int moved = transport.addEssentia(aspect, requested, targetFace);
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
            int remainder = source.addToContainer(aspect, requested);
            int moved = requested - Math.min(requested, QIOStorageTransferMath.nonNegativeInt(remainder));
            if (moved > 0) {
                markDirtySafely(target);
            }
            return moved;
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
