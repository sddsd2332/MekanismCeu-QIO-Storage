package mekceuqiostorage.smoke;

import hellfirepvp.astralsorcery.common.auxiliary.link.ILinkableTile;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import hellfirepvp.astralsorcery.common.starlight.IStarlightSource;
import hellfirepvp.astralsorcery.common.starlight.WorldNetworkHandler;
import hellfirepvp.astralsorcery.common.starlight.network.StarlightNetworkRegistry;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceTransferAdapterRegistry;
import mekanism.common.MekanismBlocks;
import mekanism.common.content.qio.QIODriveDefinition;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekanism.common.content.qio.filter.QIOResourceFilter;
import mekanism.common.frequency.Frequency.FrequencyIdentity;
import mekanism.common.frequency.FrequencyType;
import mekanism.common.security.ISecurityTile.SecurityMode;
import mekanism.common.tile.qio.TileEntityQIOComponent;
import mekanism.common.tile.qio.TileEntityQIODriveArray;
import mekanism.common.tile.qio.TileEntityQIOExporter;
import mekanism.common.tile.qio.TileEntityQIOImporter;
import mekceuqiostorage.common.QIOStorage;
import mekceuqiostorage.common.content.qio.QIOStorageDescriptors;
import mekceuqiostorage.common.content.qio.QIOStorageResourceSpecs;
import mekceuqiostorage.common.integration.astralsorcery.AstralSorceryStarlightEndpoint;
import mekceuqiostorage.common.integration.astralsorcery.AstralSorceryStarlightMath;
import mekceuqiostorage.common.integration.astralsorcery.AstralSorceryStarlightTransferAdapter;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.registration.QIOStorageBootstrap;
import mekceuqiostorage.common.registration.QIOStorageItems;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.UUID;

import static mekceuqiostorage.smoke.QIOStorageSmokeMod.check;

final class AstralSorcerySmokeTest {

    private static final BlockPos EXPORTER = new BlockPos(2, 20, 2);
    private static final BlockPos ARRAY = EXPORTER.north(3);
    private static final BlockPos IMPORTER = ARRAY.east();
    private static final BlockPos EAST_ALTAR = EXPORTER.east();
    private static final BlockPos WEST_ALTAR = EXPORTER.west();
    private static TileEntityQIOExporter exporter;
    private static TileEntityQIOImporter importer;
    private static QIOFrequency frequency;
    private static UUID resource;
    private static long beforeRotation;

    private AstralSorcerySmokeTest() {
    }

    static boolean tick(WorldServer world, int tick) {
        if (Boolean.getBoolean("mekceuqiostorage.smoke.reload")) {
            world.getChunk(ARRAY);
            if (tick < 20) {
                return false;
            }
            TileEntityQIODriveArray array = (TileEntityQIODriveArray) world.getTileEntity(ARRAY);
            check(array != null && array.getQIOFrequency() != null, "Saved drive array/frequency missing");
            check(array.getQIOFrequency().getStored(QIOResourceTypeRegistry.INSTANCE.getOrTrack(
                  QIOStorageDescriptors.starlight())) == 777, "Saved starlight balance changed");
            check(array.getQIOFrequency().getResourceEntries().size() == 1, "Saved starlight split into multiple types");
            return true;
        }
        if (tick == 0) {
            setup(world);
        } else if (tick == 20) {
            check(stored() == 3_000, "Exporter ignored its nonmatching resource filter");
            check(altar(world, EAST_ALTAR).getStarlightStored() == 0, "Filtered exporter emitted starlight");
            exporter.setFilter(new QIOResourceFilter(QIOStorageDescriptors.starlight()));
        } else if (tick == 45) {
            check(stored() < 3_000, "Normal exporter ticks did not extract QIO starlight");
            check(altar(world, EAST_ALTAR).getStarlightStored() > 0, "Adjacent altar did not receive native starlight");
            check(altar(world, WEST_ALTAR).getStarlightStored() == 0, "Exporter emitted through the wrong face");
            beforeRotation = stored();
            QIOStorage.LOGGER.info("AS smoke: normal ticks emitted {} QIO starlight to the output face", 3_000 - stored());
            exporter.setFacing(EnumFacing.EAST);
        } else if (tick == 65) {
            check(stored() < beforeRotation && altar(world, WEST_ALTAR).getStarlightStored() > 0,
                  "Rotated exporter did not use its normal output face");
            finish(world);
            return true;
        }
        return false;
    }

    private static void setup(WorldServer world) {
        check(QIOResourceTransferAdapterRegistry.INSTANCE.get(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT.getCodecId()) ==
              AstralSorceryStarlightTransferAdapter.INSTANCE, "AS transfer adapter not registered");
        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            ItemQIOStorageDrive drive = QIOStorageItems.getDrive(
                  QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT.getCodecId(), tier.getDefinition());
            check(drive != null, "Missing AS drive: " + tier);
            check(drive.getTypeCapacity(new ItemStack(drive)) == 1, "AS drive must store one generic resource type");
        }
        check(QIOStorageBootstrap.getSpecialization(QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT)
              .accepts(QIOStorageDescriptors.starlight()), "AS specialization rejects starlight");

        place(world, ARRAY, MekanismBlocks.QIO_DRIVE_ARRAY);
        place(world, IMPORTER, MekanismBlocks.QIO_IMPORTER);
        place(world, EXPORTER, MekanismBlocks.QIO_EXPORTER);
        place(world, EAST_ALTAR, BlocksAS.blockAltar);
        place(world, WEST_ALTAR, BlocksAS.blockAltar);
        TileEntityQIODriveArray array = (TileEntityQIODriveArray) world.getTileEntity(ARRAY);
        importer = (TileEntityQIOImporter) world.getTileEntity(IMPORTER);
        exporter = (TileEntityQIOExporter) world.getTileEntity(EXPORTER);
        check(!(exporter instanceof IStarlightSource) && !(exporter instanceof ILinkableTile),
              "Legacy exporter interfaces are still mixed in");
        exporter.setFacing(EnumFacing.WEST);
        exporter.setFilter(new QIOResourceFilter(QIOStorageDescriptors.mana()));
        UUID owner = UUID.randomUUID();
        FrequencyIdentity identity = new FrequencyIdentity("AS smoke " + owner, SecurityMode.PUBLIC, owner);
        ItemQIOStorageDrive drive = QIOStorageItems.getDrive(
              QIOStorageResourceSpecs.ASTRALSORCERY_STARLIGHT.getCodecId(), QIODriveDefinition.BASE);
        array.updateQIODriveStack(0, new ItemStack(drive));
        for (TileEntityQIOComponent tile : new TileEntityQIOComponent[]{array, importer, exporter}) {
            tile.getSecurity().setOwnerUUID(owner);
            tile.setFrequency(FrequencyType.QIO, identity, owner);
        }
        frequency = array.getQIOFrequency();
        check(frequency != null && frequency == importer.getQIOFrequency() && frequency == exporter.getQIOFrequency(),
              "QIO components did not join the same frequency");
        resource = QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.starlight());
        for (IWeakConstellation constellation : new IWeakConstellation[]{null, Constellations.aevitas, Constellations.vicio}) {
            StarlightNetworkRegistry.IStarlightBlockHandler handler = StarlightNetworkRegistry.getStarlightHandler(
                  world, IMPORTER, world.getBlockState(IMPORTER), constellation);
            check(handler == AstralSorceryStarlightEndpoint.INSTANCE, "AS did not discover the importer endpoint");
            handler.receiveStarlight(world, world.rand, IMPORTER, constellation, 5.0);
        }
        check(stored() == 3_000 && frequency.getResourceEntries().size() == 1,
              "Importer did not merge constellation-specific input into generic QIO starlight");
        check(frequency.massInsert(QIOStorageDescriptors.mana(), 1, Action.SIMULATE) == 0,
              "Starlight-only drive accepted a foreign codec");
        importer.setFilter(new QIOResourceFilter(QIOStorageDescriptors.mana()));
        AstralSorceryStarlightEndpoint.INSTANCE.receiveStarlight(world, world.rand, IMPORTER, null, 5.0);
        check(stored() == 3_000, "AS input bypassed the importer filter");
        importer.clearFilters();
        QIOStorage.LOGGER.info("AS smoke: four drive tiers and native input verified; starting normal exporter ticks");
    }

    private static void finish(WorldServer world) {
        TileAltar full = altar(world, WEST_ALTAR);
        full.receiveStarlight(null, AstralSorceryStarlightMath.toNetworkAmount(full.getMaxStarlightStorage()));
        check(full.getStarlightStored() == full.getMaxStarlightStorage(), "Cannot fill the test altar");
        check(emitOnce() == 64, "Full altar did not consume the normal 64-unit exporter batch");
        check(full.getStarlightStored() == full.getMaxStarlightStorage(), "AS altar exceeded native capacity");
        place(world, WEST_ALTAR, BlocksAS.starlightInfuser);
        check(emitOnce() == 64, "No-op infuser did not consume emitted QIO starlight");
        check(WorldNetworkHandler.getNetworkHandler(world).getSourceAt(EXPORTER) == null &&
              WorldNetworkHandler.getNetworkHandler(world).getTransmissionNode(EXPORTER) == null,
              "Exporter was added to the AS transmission network");

        TileAltar focused = altar(world, EAST_ALTAR);
        ItemStack crystal = new ItemStack(ItemsAS.tunedRockCrystal);
        ItemTunedCrystalBase.applyMainConstellation(crystal, Constellations.vicio);
        focused.setFocusStack(crystal);
        check(focused.getFocusedConstellation() == Constellations.vicio,
              "Native altar crystal metadata did not resolve to its main constellation");
        focused.setFocusStack(ItemStack.EMPTY);

        exporter.clearFilters();
        exporter.setExportWithoutFilter(false);
        frequency.massExtract(resource, Long.MAX_VALUE, Action.EXECUTE);
        check(frequency.massInsert(QIOStorageDescriptors.starlight(), 777, Action.EXECUTE) == 777,
              "Cannot seed persistence verification balance");
        QIOStorage.LOGGER.info("AS smoke: output faces, full altar and no-op receiver verified; saving 777 starlight");
    }

    private static long emitOnce() {
        long before = stored();
        // Keep the full-altar check within one world tick so AS passive decay cannot affect it.
        for (int i = 0; i < 12 && stored() == before; i++) {
            exporter.doRestrictedTick();
        }
        return before - stored();
    }

    private static long stored() {
        return frequency.getStored(resource);
    }

    private static TileAltar altar(WorldServer world, BlockPos pos) {
        return (TileAltar) world.getTileEntity(pos);
    }

    private static void place(WorldServer world, BlockPos pos, Block block) {
        world.setBlockToAir(pos);
        check(world.setBlockState(pos, block.getDefaultState()), "Cannot place smoke block at " + pos);
    }
}
