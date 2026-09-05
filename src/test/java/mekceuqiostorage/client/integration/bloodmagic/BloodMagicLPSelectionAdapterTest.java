package mekceuqiostorage.client.integration.bloodmagic;

import WayofTime.bloodmagic.core.data.Binding;
import WayofTime.bloodmagic.iface.IBindable;
import WayofTime.bloodmagic.orb.BloodOrb;
import WayofTime.bloodmagic.orb.IBloodOrb;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekceuqiostorage.common.content.qio.QIOStorageCodecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMagicLPSelectionAdapterTest {

    private static final UUID OWNER = UUID.fromString(
          "10000000-0000-0000-0000-000000000001");

    @BeforeAll
    static void registerCodec() {
        Bootstrap.register();
        QIOStorageBootstrap.registerCodecs();
    }

    @Test
    void boundBloodOrbSelectsItsOwnersSoulNetwork() {
        ItemStack orb = boundOrb(OWNER, "Alice");

        List<QIOResourceDescriptor> contained = BloodMagicLPSelectionAdapter.INSTANCE
              .getContainedResources(orb);
        assertEquals(1, contained.size());
        SoulNetworkLP resource = contained.get(0).resolve(QIOStorageCodecs.BLOODMAGIC_LP);
        assertNotNull(resource);
        assertEquals(OWNER, resource.getOwnerId());
        assertEquals("Alice", resource.getOwnerName());
        assertEquals(contained.get(0), BloodMagicLPSelectionAdapter.INSTANCE.fromIngredient(orb));
    }

    @Test
    void unboundOrNonOrbItemsDoNotClaimLp() {
        ItemStack unboundOrb = new ItemStack(new TestBloodOrbItem(true));
        ItemStack boundNonOrb = new ItemStack(new TestBloodOrbItem(false));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("binding", new Binding(OWNER, "Alice").serializeNBT());
        boundNonOrb.setTagCompound(tag);

        assertTrue(BloodMagicLPSelectionAdapter.INSTANCE.getContainedResources(
              ItemStack.EMPTY).isEmpty());
        assertTrue(BloodMagicLPSelectionAdapter.INSTANCE.getContainedResources(
              unboundOrb).isEmpty());
        assertTrue(BloodMagicLPSelectionAdapter.INSTANCE.getContainedResources(
              boundNonOrb).isEmpty());
        assertNull(BloodMagicLPSelectionAdapter.INSTANCE.fromIngredient(new Object()));
    }

    private static ItemStack boundOrb(UUID ownerId, String ownerName) {
        ItemStack stack = new ItemStack(new TestBloodOrbItem(true));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("binding", new Binding(ownerId, ownerName).serializeNBT());
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class TestBloodOrbItem extends Item implements IBloodOrb, IBindable {

        private final BloodOrb orb;

        private TestBloodOrbItem(boolean exposesOrb) {
            orb = exposesOrb ? new BloodOrb("test", 1, 1_000, 20) : null;
        }

        @Override
        public BloodOrb getOrb(ItemStack stack) {
            return orb;
        }
    }
}
