package mekceuqiostorage.common.integration.bloodmagic;

import WayofTime.bloodmagic.core.data.SoulNetwork;
import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceStack;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.content.qio.QIOStorageResources.SoulNetworkLP;
import mekceuqiostorage.common.integration.transfer.AbstractQIOResourceTransferAdapter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/** Transfers owner-bound Soul Network LP through a Blood Altar containing a bound Blood Orb. */
public final class BloodMagicLPTransferAdapter
      extends AbstractQIOResourceTransferAdapter<SoulNetworkLP> {

    public static final BloodMagicLPTransferAdapter INSTANCE = new BloodMagicLPTransferAdapter();

    private BloodMagicLPTransferAdapter() {
        super(QIOStorageResourceSpecs.BLOODMAGIC_LP);
    }

    @Override
    public boolean supports(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace) {
        return BloodMagicSoulNetworkEndpoint.resolve(target) != null;
    }

    @Override
    @Nonnull
    public List<QIOResourceStack> getExtractable(@Nonnull TileEntity target,
          @Nonnull EnumFacing targetFace, int maximumTypes, long maximumAmount) {
        if (maximumTypes <= 0 || maximumAmount <= 0) {
            return Collections.emptyList();
        }
        BloodMagicSoulNetworkEndpoint endpoint = BloodMagicSoulNetworkEndpoint.resolve(target);
        SoulNetwork network = endpoint == null ? null : endpoint.getNetwork();
        if (endpoint == null) {
            return Collections.emptyList();
        }
        long available = BloodMagicSoulNetworkTransfer.getExtractable(network,
              endpoint.getTransferLimit());
        return candidate(endpoint.getResource(), available, maximumTypes, maximumAmount);
    }

    @Override
    protected long extractResolved(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          @Nonnull SoulNetworkLP resource, long amount, @Nonnull Action action) {
        BloodMagicSoulNetworkEndpoint endpoint = BloodMagicSoulNetworkEndpoint.resolve(target);
        SoulNetwork network = endpoint == null ? null : endpoint.getNetwork();
        if (endpoint == null || !endpoint.owns(resource)) {
            return 0;
        }
        long moved = BloodMagicSoulNetworkTransfer.extract(network, amount,
              endpoint.getTransferLimit(), action, endpoint::ticket);
        if (moved > 0 && action.execute()) {
            markDirtySafely(endpoint.getAltar());
        }
        return moved;
    }

    @Override
    protected long insertResolved(@Nonnull TileEntity target, @Nonnull EnumFacing targetFace,
          @Nonnull SoulNetworkLP resource, long amount, @Nonnull Action action) {
        BloodMagicSoulNetworkEndpoint endpoint = BloodMagicSoulNetworkEndpoint.resolve(target);
        SoulNetwork network = endpoint == null ? null : endpoint.getNetwork();
        int maximum = endpoint == null ? 0 : endpoint.getMaximum();
        if (endpoint == null || !endpoint.owns(resource)) {
            return 0;
        }
        long moved = BloodMagicSoulNetworkTransfer.insert(network, amount, maximum,
              endpoint.getTransferLimit(), action, endpoint::ticket);
        if (moved > 0 && action.execute()) {
            markDirtySafely(endpoint.getAltar());
        }
        return moved;
    }
}
