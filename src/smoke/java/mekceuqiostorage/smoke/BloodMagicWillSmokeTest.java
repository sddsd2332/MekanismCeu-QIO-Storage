package mekceuqiostorage.smoke;

import WayofTime.bloodmagic.core.RegistrarBloodMagicItems;
import WayofTime.bloodmagic.soul.IDemonWillGem;
import WayofTime.bloodmagic.core.RegistrarBloodMagicBlocks;
import WayofTime.bloodmagic.soul.EnumDemonWillType;
import WayofTime.bloodmagic.tile.TileDemonCrucible;
import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceTransferAdapterRegistry;
import mekanism.common.MekanismBlocks;
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
import mekceuqiostorage.common.content.qio.QIOStorageResources.DemonWill;
import mekceuqiostorage.common.integration.bloodmagic.BloodMagicWillTransferAdapter;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.registration.QIOStorageItems;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.UUID;

import static mekceuqiostorage.smoke.QIOStorageSmokeMod.check;

final class BloodMagicWillSmokeTest {

    private static final BlockPos ARRAY = new BlockPos(2, 20, 2);
    private static final BlockPos SOURCE = ARRAY.east(4);
    private static final BlockPos IMPORTER = SOURCE.west();
    private static final BlockPos SINK = ARRAY.east(48);
    private static final BlockPos EXPORTER = SINK.west();
    private static TileEntityQIODriveArray array;
    private static TileEntityQIOImporter importer;
    private static TileEntityQIOExporter exporter;
    private static TileDemonCrucible source;
    private static TileDemonCrucible sink;
    private static QIOFrequency frequency;

    private BloodMagicWillSmokeTest() {
    }

    static boolean tick(WorldServer world, int tick) {
        if (Boolean.getBoolean("mekceuqiostorage.smoke.reload")) {
            world.getChunk(ARRAY);
            world.getChunk(SINK);
            if (tick < 20) {
                return false;
            }
            array = (TileEntityQIODriveArray) world.getTileEntity(ARRAY);
            check(array != null && array.getQIOFrequency() != null, "Saved Will array/frequency missing");
            frequency = array.getQIOFrequency();
            for (DemonWill type : DemonWill.values()) {
                check(stored(type) == 123, "Saved Will balance changed for " + type);
            }
            check(frequency.getResourceEntries().size() == 5, "Saved Will identities changed");
            checkHints(5);
            return true;
        }
        if (tick == 0) {
            setup(world);
        } else if (tick == 60) {
            check(stored(DemonWill.STEADFAST) == 1, "Filtered fifth Will type was never discovered");
            for (DemonWill type : DemonWill.values()) {
                if (type != DemonWill.STEADFAST) {
                    check(stored(type) == 0, "Importer ignored Will filter for " + type);
                }
            }
            importer.clearFilters();
            QIOStorage.LOGGER.info("Will smoke: filtered fifth type imported; importing all types");
        } else if (tick == 120) {
            for (DemonWill type : DemonWill.values()) {
                check(stored(type) == 1, "Will import balance incorrect for " + type);
                near(0.295, source.getCurrentWill(nativeType(type)), "Source remainder lost for " + type);
            }
            check(frequency.getResourceEntries().size() == 5, "Will types merged in QIO");
            checkHints(5);
            exporter.setFilter(new QIOResourceFilter(QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE)));
        } else if (tick == 160) {
            for (DemonWill type : DemonWill.values()) {
                check(stored(type) == (type == DemonWill.DESTRUCTIVE ? 0 : 1), "Exporter ignored Will filter");
                near(type == DemonWill.DESTRUCTIVE ? 1 : 0, sink.getCurrentWill(nativeType(type)), "Wrong Will delivered");
            }
            exporter.clearFilters();
            exporter.setExportWithoutFilter(true);
        } else if (tick == 210) {
            check(frequency.getResourceEntries().isEmpty(), "Will exporter did not finish the transfer");
            for (DemonWill type : DemonWill.values()) {
                near(1, sink.getCurrentWill(nativeType(type)), "Will export amount incorrect for " + type);
            }
            checkHints(0);
            finish();
        } else if (tick == 230) {
            checkHints(5);
            return true;
        }
        return false;
    }

    private static void setup(WorldServer world) {
        check(QIOResourceTransferAdapterRegistry.INSTANCE.get(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId()) ==
              BloodMagicWillTransferAdapter.INSTANCE, "Will transfer adapter missing");
        for (QIOStorageDriveTier tier : QIOStorageDriveTier.values()) {
            ItemQIOStorageDrive lp = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BLOODMAGIC_LP.getCodecId(), tier.getDefinition());
            ItemQIOStorageDrive will = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId(), tier.getDefinition());
            check(lp != null && will != null && lp != will, "LP and Will must have separate physical drives");
            check(lp.getTypeCapacity(new ItemStack(lp)) == 1, "LP drive type capacity changed");
            check(will.getTypeCapacity(new ItemStack(will)) == 5, "Will drive must hold five types");
        }
        place(world, ARRAY, MekanismBlocks.QIO_DRIVE_ARRAY);
        place(world, IMPORTER, MekanismBlocks.QIO_IMPORTER);
        place(world, EXPORTER, MekanismBlocks.QIO_EXPORTER);
        place(world, SOURCE, RegistrarBloodMagicBlocks.DEMON_CRUCIBLE);
        place(world, SINK, RegistrarBloodMagicBlocks.DEMON_CRUCIBLE);
        array = (TileEntityQIODriveArray) world.getTileEntity(ARRAY);
        importer = (TileEntityQIOImporter) world.getTileEntity(IMPORTER);
        exporter = (TileEntityQIOExporter) world.getTileEntity(EXPORTER);
        source = (TileDemonCrucible) world.getTileEntity(SOURCE);
        sink = (TileDemonCrucible) world.getTileEntity(SINK);
        importer.setFacing(EnumFacing.WEST);
        exporter.setFacing(EnumFacing.WEST);
        importer.setFilter(new QIOResourceFilter(QIOStorageDescriptors.will(DemonWill.STEADFAST)));
        exporter.setExportWithoutFilter(false);
        ItemQIOStorageDrive drive = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BLOODMAGIC_WILL.getCodecId(),
              QIOStorageDriveTier.BASE.getDefinition());
        array.updateQIODriveStack(0, new ItemStack(drive));
        UUID owner = UUID.randomUUID();
        FrequencyIdentity identity = new FrequencyIdentity("Will smoke " + owner, SecurityMode.PUBLIC, owner);
        for (TileEntityQIOComponent tile : new TileEntityQIOComponent[]{array, importer, exporter}) {
            tile.getSecurity().setOwnerUUID(owner);
            tile.setFrequency(FrequencyType.QIO, identity, owner);
        }
        frequency = array.getQIOFrequency();
        check(frequency != null, "Will frequency missing");
        check(frequency.massInsert(QIOStorageDescriptors.lp(owner), 1, Action.SIMULATE) == 0, "Will disk accepted LP");
        for (DemonWill type : DemonWill.values()) {
            source.fillDemonWill(nativeType(type), 1.295, true);
        }
        source.markDirty();
        QIOStorage.LOGGER.info("Will smoke: independent LP/Will tiers verified; testing real importer ticks");
    }

    private static void finish() {
        sink.fillDemonWill(EnumDemonWillType.DEFAULT, 100, true);
        check(frequency.massInsert(QIOStorageDescriptors.will(DemonWill.DEFAULT), 100, Action.EXECUTE) == 100,
              "Cannot seed full-sink test");
        exporter.setFilter(new QIOResourceFilter(QIOStorageDescriptors.will(DemonWill.DEFAULT)));
        for (int i = 0; i < 12; i++) {
            exporter.doRestrictedTick();
        }
        check(stored(DemonWill.DEFAULT) == 100, "Full Will sink consumed QIO resources");
        sink.drainDemonWill(EnumDemonWillType.DEFAULT, 25, true);
        for (int i = 0; i < 12; i++) {
            exporter.doRestrictedTick();
        }
        check(stored(DemonWill.DEFAULT) == 75, "Will capacity simulation/rollback was incorrect");
        near(100, sink.getCurrentWill(EnumDemonWillType.DEFAULT), "Will sink overfilled");
        frequency.massExtract(id(DemonWill.DEFAULT), Long.MAX_VALUE, Action.EXECUTE);

        ItemStack gemStack = new ItemStack(RegistrarBloodMagicItems.SOUL_GEM);
        IDemonWillGem gem = (IDemonWillGem) gemStack.getItem();
        NBTTagCompound gemTag = new NBTTagCompound();
        gemTag.setString("demonWillType", "destructive");
        gemTag.setDouble("souls", 5.25D);
        gemStack.setTagCompound(gemTag);
        sink.setInventorySlotContents(0, gemStack);
        check(sink.willMap.isEmpty(), "Soul Gem test unexpectedly populated crucible willMap");
        long gemSimulated = BloodMagicWillTransferAdapter.INSTANCE.extract(sink, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), 100, Action.SIMULATE);
        check(gemSimulated == 5, "Soul Gem Will was not visible to the importer simulation");
        long gemMoved = BloodMagicWillTransferAdapter.INSTANCE.extract(sink, EnumFacing.NORTH,
              QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), gemSimulated, Action.EXECUTE);
        check(gemMoved == 5, "Soul Gem Will was not extracted through the native gem API");
        near(0.25, gem.getWill(EnumDemonWillType.DESTRUCTIVE, gemStack),
              "Soul Gem native remainder lost");
        check(sink.willMap.isEmpty(), "Soul Gem import incorrectly created a crucible willMap entry");
        check(frequency.massInsert(QIOStorageDescriptors.will(DemonWill.DESTRUCTIVE), gemMoved,
              Action.EXECUTE) == gemMoved, "Soul Gem Will was not inserted into QIO");
        check(stored(DemonWill.DESTRUCTIVE) == 5, "Soul Gem Will QIO balance incorrect");
        frequency.massExtract(id(DemonWill.DESTRUCTIVE), Long.MAX_VALUE, Action.EXECUTE);
        sink.setInventorySlotContents(0, ItemStack.EMPTY);

        importer.clearFilters();
        importer.setImportWithoutFilter(false);
        exporter.clearFilters();
        exporter.setExportWithoutFilter(false);
        for (DemonWill type : DemonWill.values()) {
            check(frequency.massInsert(QIOStorageDescriptors.will(type), 123, Action.EXECUTE) == 123,
                  "Cannot seed saved Will balance for " + type);
        }
        QIOStorage.LOGGER.info("Will smoke: five-type transfers and full sink verified; saving 123 units per type");
    }

    private static void checkHints(int expected) {
        ItemStack stack = array.getQIODriveStacks().get(0);
        check(((ItemQIOStorageDrive) stack.getItem()).getWillResources(stack).size() == expected,
              "Will drive selection metadata differs from stored contents");
    }

    private static long stored(DemonWill type) {
        return frequency.getStored(id(type));
    }

    private static UUID id(DemonWill type) {
        return QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.will(type));
    }

    private static EnumDemonWillType nativeType(DemonWill type) {
        return EnumDemonWillType.valueOf(type.name());
    }

    private static void near(double expected, double actual, String message) {
        check(Double.isFinite(actual) && Math.abs(expected - actual) < 1E-10,
              message + ": expected " + expected + ", got " + actual);
    }

    private static void place(WorldServer world, BlockPos pos, Block block) {
        world.setBlockToAir(pos);
        check(world.setBlockState(pos, block.getDefaultState()), "Cannot place smoke block at " + pos);
    }
}
