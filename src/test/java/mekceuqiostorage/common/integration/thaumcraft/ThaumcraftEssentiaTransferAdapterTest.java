package mekceuqiostorage.common.integration.thaumcraft;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("optional-mod-runtime")
class ThaumcraftEssentiaTransferAdapterTest {
    private static final ThaumcraftEssentiaTransferAdapter ADAPTER = ThaumcraftEssentiaTransferAdapter.INSTANCE;

    @BeforeAll
    static void register() {
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void rejectedLeadingTypesDoNotStarveFilteredOrCapacityLimitedResources() {
        for (boolean mirror : new boolean[]{false, true}) {
            Source source = new Source(mirror);
            source.contents.add(Aspect.AIR, 1000).add(Aspect.WATER, 1000).add(Aspect.FIRE, 1000);
            Set<String> seen = new HashSet<>();
            for (int pass = 0; pass < 9; pass++) {
                List<QIOResourceStack> candidates = ADAPTER.getExtractable(source, EnumFacing.NORTH, 1, 64);
                assertEquals(1, candidates.size());
                String tag = tag(candidates.get(0));
                seen.add(tag);
                // Models both filtering and QIO refusing the first candidates: leave them intact.
                if (Aspect.WATER.getTag().equals(tag)) {
                    long moved = ADAPTER.extract(source, EnumFacing.NORTH,
                          candidates.get(0).getDescriptor(), candidates.get(0).getAmount(), Action.EXECUTE);
                    assertTrue(moved > 0);
                }
            }
            assertEquals(3, seen.size());
            assertEquals(1000, source.contents.getAmount(Aspect.AIR));
            assertEquals(1000, source.contents.getAmount(Aspect.FIRE));
            assertTrue(source.contents.getAmount(Aspect.WATER) < 1000);
        }
    }

    @Test
    void discoveryBoundsEveryPassAndVisitsMoreTypesThanTheBudgetWithoutMutating() {
        Source source = new Source(false);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            source.contents.add(aspect, 1000);
        }
        Set<String> seen = new HashSet<>();
        for (int pass = 0; pass < 12; pass++) {
            List<QIOResourceStack> candidates = ADAPTER.getExtractable(source, EnumFacing.EAST, 4, 3);
            assertTrue(candidates.size() <= 3);
            assertTrue(candidates.stream().mapToLong(QIOResourceStack::getAmount).sum() <= 3);
            for (QIOResourceStack candidate : candidates) {
                seen.add(tag(candidate));
                assertTrue(ADAPTER.extract(source, EnumFacing.EAST, candidate.getDescriptor(),
                      candidate.getAmount(), Action.SIMULATE) > 0);
            }
        }
        assertEquals(Aspect.getPrimalAspects().size(), seen.size());
        assertEquals(0, source.mutations);
    }

    @Test
    void cursorBelongsToExactSourceAndFaceEvenWhenTilesCompareEqual() {
        Source first = new Source(false);
        Source replacement = new Source(false);
        first.contents.add(Aspect.AIR, 100).add(Aspect.WATER, 100).add(Aspect.FIRE, 100);
        replacement.contents = first.contents.copy();
        String initial = tag(ADAPTER.getExtractable(first, EnumFacing.NORTH, 1, 1).get(0));
        assertNotEquals(initial, tag(ADAPTER.getExtractable(first, EnumFacing.NORTH, 1, 1).get(0)));
        assertEquals(initial, tag(ADAPTER.getExtractable(first, EnumFacing.SOUTH, 1, 1).get(0)));
        assertEquals(initial, tag(ADAPTER.getExtractable(replacement, EnumFacing.NORTH, 1, 1).get(0)));
    }

    @Test
    void invalidSourceCannotPublishOrTransferResources() {
        Source source = new Source(false);
        source.contents.add(Aspect.AIR, 100);
        source.invalidate();
        assertFalse(ADAPTER.supports(source, EnumFacing.NORTH));
        assertTrue(ADAPTER.getExtractable(source, EnumFacing.NORTH, 1, 64).isEmpty());
        assertEquals(0, ADAPTER.extract(source, EnumFacing.NORTH,
              QIOStorageDescriptors.essentia(Aspect.AIR.getTag()), 64, Action.EXECUTE));
        assertEquals(0, source.mutations);
    }

    @Test
    void deletingTheNextCandidateDoesNotRestartAtTheFirstType() {
        Source source = new Source(false);
        source.contents.add(Aspect.AIR, 100).add(Aspect.WATER, 100).add(Aspect.FIRE, 100);
        List<QIOResourceStack> order = ADAPTER.getExtractable(source, EnumFacing.DOWN, 3, 3);
        String first = tag(order.get(0));
        String next = tag(order.get(1));
        String last = tag(order.get(2));
        assertEquals(first, tag(ADAPTER.getExtractable(source, EnumFacing.DOWN, 1, 1).get(0)));
        source.contents.remove(Aspect.getAspect(next));
        assertEquals(last, tag(ADAPTER.getExtractable(source, EnumFacing.DOWN, 1, 1).get(0)));
        source.contents.add(Aspect.getAspect(next), 100);
        Set<String> visited = new HashSet<>();
        for (int pass = 0; pass < 6; pass++) {
            visited.add(tag(ADAPTER.getExtractable(source, EnumFacing.DOWN, 1, 1).get(0)));
        }
        assertEquals(3, visited.size());
    }

    private static String tag(QIOResourceStack stack) {
        return stack.getDescriptor().resolve(QIOStorageCodecs.THAUMCRAFT_ESSENTIA).getAspectTag();
    }

    private static final class Source extends TileEntity implements IAspectSource {
        private AspectList contents = new AspectList();
        private final boolean mirror;
        private int mutations;

        Source(boolean mirror) { this.mirror = mirror; }
        @Override public boolean isBlocked() { return false; }
        @Override public AspectList getAspects() { return mirror ? new AspectList() : contents.copy(); }
        @Override public void setAspects(AspectList aspects) { contents = aspects; mutations++; }
        @Override public boolean doesContainerAccept(Aspect aspect) { return true; }
        @Override public int addToContainer(Aspect aspect, int amount) { contents.add(aspect, amount); mutations++; return 0; }
        @Override public boolean takeFromContainer(Aspect aspect, int amount) {
            if (!doesContainerContainAmount(aspect, amount)) return false;
            contents.reduce(aspect, amount); mutations++; return true;
        }
        @Override public boolean takeFromContainer(AspectList aspects) { throw new UnsupportedOperationException(); }
        @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return contents.getAmount(aspect) >= amount; }
        @Override public boolean doesContainerContain(AspectList aspects) { throw new UnsupportedOperationException(); }
        @Override public int containerContains(Aspect aspect) { return mirror ? 0 : contents.getAmount(aspect); }
        @Override public boolean equals(Object other) { return other instanceof Source; }
        @Override public int hashCode() { return 1; }
    }
}
