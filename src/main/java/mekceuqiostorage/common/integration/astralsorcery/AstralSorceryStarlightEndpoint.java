package mekceuqiostorage.common.integration.astralsorcery;

import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.starlight.network.StarlightNetworkRegistry;
import mekanism.common.MekanismBlocks;
import mekanism.common.tile.qio.TileEntityQIOImporter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/** Receives native AS network output at a QIO importer linked with the AS linker. */
public final class AstralSorceryStarlightEndpoint
      implements StarlightNetworkRegistry.IStarlightBlockHandler {

    public static final AstralSorceryStarlightEndpoint INSTANCE =
          new AstralSorceryStarlightEndpoint();

    private AstralSorceryStarlightEndpoint() {
    }

    @Override
    public boolean isApplicable(@Nonnull World world, @Nonnull BlockPos pos,
          @Nonnull IBlockState state) {
        return state.getBlock() == MekanismBlocks.QIO_IMPORTER &&
              world.getTileEntity(pos) instanceof TileEntityQIOImporter;
    }

    @Override
    public boolean isApplicable(@Nonnull World world, @Nonnull BlockPos pos,
          @Nonnull IBlockState state, @Nullable IWeakConstellation starlightType) {
        // The QIO resource is deliberately constellation-agnostic. The AS type is transport
        // metadata and is intentionally not used to split the stored resource.
        return isApplicable(world, pos, state);
    }

    @Override
    public void receiveStarlight(@Nonnull World world, @Nonnull Random random,
          @Nonnull BlockPos pos, @Nullable IWeakConstellation starlightType, double amount) {
        if (world.isRemote || !world.isBlockLoaded(pos)) {
            return;
        }
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityQIOImporter) {
            AstralSorceryQIOTransfer.receive((TileEntityQIOImporter) tile, amount);
        }
    }
}
