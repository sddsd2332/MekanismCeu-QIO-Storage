package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.altar.AltarTier;
import WayofTime.bloodmagic.core.data.Binding;
import WayofTime.bloodmagic.core.data.SoulNetwork;
import WayofTime.bloodmagic.core.data.SoulTicket;
import WayofTime.bloodmagic.iface.IBindable;
import WayofTime.bloodmagic.orb.BloodOrb;
import WayofTime.bloodmagic.orb.IBloodOrb;
import WayofTime.bloodmagic.tile.TileAltar;
import WayofTime.bloodmagic.util.helper.NetworkHelper;
import com.mojang.authlib.GameProfile;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** A Blood Altar whose bound orb explicitly selects one player's Soul Network. */
final class BloodMagicSoulNetworkEndpoint {

    private static final int QIO_TRANSFER_TICKS = 10;

    private final TileAltar altar;
    private final Binding binding;
    private final BloodOrb orb;
    private final SoulNetworkLP resource;

    private BloodMagicSoulNetworkEndpoint(TileAltar altar, Binding binding, BloodOrb orb,
          SoulNetworkLP resource) {
        this.altar = altar;
        this.binding = binding;
        this.orb = orb;
        this.resource = resource;
    }

    @Nullable
    static BloodMagicSoulNetworkEndpoint resolve(@Nonnull TileEntity target) {
        if (!(target instanceof TileAltar) || target.isInvalid() || target.getWorld() == null ||
              target.getWorld().isRemote) {
            return null;
        }
        try {
            TileAltar altar = (TileAltar) target;
            ItemStack stack = altar.getStackInSlot(0);
            if (stack.isEmpty()) {
                return null;
            }
            Item item = stack.getItem();
            if (!(item instanceof IBloodOrb) || !(item instanceof IBindable)) {
                return null;
            }
            BloodOrb orb = ((IBloodOrb) item).getOrb(stack);
            Binding binding = ((IBindable) item).getBinding(stack);
            AltarTier altarTier = altar.getTier();
            if (orb == null || binding == null || altarTier == null ||
                  altarTier.toInt() < orb.getTier() || orb.getCapacity() <= 0 ||
                  orb.getFillRate() <= 0) {
                return null;
            }
            UUID ownerId = binding.getOwnerId();
            SoulNetworkLP resource = SoulNetworkLP.of(ownerId,
                  resolveOwnerName(ownerId, binding.getOwnerName()));
            return new BloodMagicSoulNetworkEndpoint(altar, binding, orb, resource);
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    @Nonnull
    SoulNetworkLP getResource() {
        return resource;
    }

    boolean owns(@Nonnull SoulNetworkLP candidate) {
        return resource.equals(candidate);
    }

    @Nullable
    SoulNetwork getNetwork() {
        try {
            if (DimensionManager.getWorld(0) == null) {
                return null;
            }
            SoulNetwork network = NetworkHelper.getSoulNetwork(binding);
            return network != null && resource.getOwnerId().equals(network.getPlayerId()) ?
                  network : null;
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    int getMaximum() {
        try {
            return maximum(orb.getCapacity(), altar.getOrbMultiplier());
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    int getTransferLimit() {
        try {
            return transferLimit(orb.getFillRate(), altar.getConsumptionMultiplier());
        } catch (LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    @Nonnull
    SoulTicket ticket(int amount) {
        return SoulTicket.block(altar.getWorld(), altar.getPos(), amount);
    }

    @Nonnull
    TileAltar getAltar() {
        return altar;
    }

    static int maximum(int orbCapacity, float orbMultiplier) {
        if (!Float.isFinite(orbMultiplier) || orbMultiplier <= 0) {
            return 0;
        }
        return positiveInt(orbCapacity * (double) orbMultiplier);
    }

    static int transferLimit(int orbFillRate, float speedBonus) {
        if (!Float.isFinite(speedBonus) || speedBonus < 0) {
            return 0;
        }
        return positiveInt(orbFillRate * (1D + speedBonus) * QIO_TRANSFER_TICKS);
    }

    private static int positiveInt(double value) {
        if (!(value > 0) || Double.isInfinite(value) || Double.isNaN(value)) {
            return 0;
        }
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.floor(value);
    }

    @Nullable
    private static String resolveOwnerName(UUID ownerId, @Nullable String bindingName) {
        try {
            String cachedName = UsernameCache.getLastKnownUsername(ownerId);
            if (cachedName != null && !cachedName.isEmpty()) {
                return cachedName;
            }
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null && server.getPlayerProfileCache() != null) {
                GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(ownerId);
                if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                    return profile.getName();
                }
            }
        } catch (LinkageError | RuntimeException ignored) {
            // Binding data remains an offline-safe fallback when the name caches are unavailable.
        }
        return bindingName;
    }
}
