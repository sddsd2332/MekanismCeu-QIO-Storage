package mekceuqiostorage.smoke;

import WayofTime.bloodmagic.core.RegistrarBloodMagic;
import WayofTime.bloodmagic.core.RegistrarBloodMagicBlocks;
import WayofTime.bloodmagic.core.RegistrarBloodMagicItems;
import WayofTime.bloodmagic.core.data.Binding;
import WayofTime.bloodmagic.core.data.SoulNetwork;
import WayofTime.bloodmagic.tile.TileAltar;
import WayofTime.bloodmagic.util.helper.NetworkHelper;
import mekanism.api.Action;
import mekanism.common.MekanismBlocks;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.content.qio.QIOResourceTypeRegistry;
import mekanism.common.content.qio.filter.QIOFilter;
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
import mekceuqiostorage.common.integration.bloodmagic.BloodMagicLPTransferAdapter;
import mekceuqiostorage.common.item.ItemQIOStorageDrive;
import mekceuqiostorage.common.registration.QIOStorageItems;
import mekceuqiostorage.common.tier.QIOStorageDriveTier;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.io.FileInputStream;
import java.util.UUID;

import static mekceuqiostorage.smoke.QIOStorageSmokeMod.check;

final class BloodMagicLPSmokeTest {

    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final BlockPos ARRAY = new BlockPos(2, 20, 2);
    private static final BlockPos ALTAR = ARRAY.east(4);
    private static TileEntityQIODriveArray array;
    private static TileEntityQIOImporter importer;
    private static TileEntityQIOExporter exporter;
    private static QIOFrequency frequency;
    private static SoulNetwork network;

    private BloodMagicLPSmokeTest() {
    }

    static boolean tick(WorldServer world, int tick) throws Exception {
        if (Boolean.getBoolean("mekceuqiostorage.smoke.reload")) {
            world.getChunk(ARRAY);
            if (tick < 20) {
                return false;
            }
            array = (TileEntityQIODriveArray) world.getTileEntity(ARRAY);
            check(array != null && array.getQIOFrequency() != null, "Saved LP array/frequency missing");
            frequency = array.getQIOFrequency();
            check(stored() == 123, "Saved LP balance changed");
            checkOwnerHint();
            return true;
        }
        if (tick == 0) {
            inspectPlayer();
            place(world, ARRAY, MekanismBlocks.QIO_DRIVE_ARRAY);
            place(world, ALTAR, RegistrarBloodMagicBlocks.BLOOD_ALTAR);
            place(world, ALTAR.west(), MekanismBlocks.QIO_IMPORTER);
            place(world, ALTAR.east(), MekanismBlocks.QIO_EXPORTER);
            array = (TileEntityQIODriveArray) world.getTileEntity(ARRAY);
            importer = (TileEntityQIOImporter) world.getTileEntity(ALTAR.west());
            exporter = (TileEntityQIOExporter) world.getTileEntity(ALTAR.east());
            importer.setFacing(EnumFacing.WEST);
            exporter.setFacing(EnumFacing.EAST);
            importer.setFilter(QIOFilter.read(new QIOResourceFilter(QIOStorageDescriptors.lp(OWNER)).write()));
            exporter.setExportWithoutFilter(false);
            ItemQIOStorageDrive drive = QIOStorageItems.getDrive(QIOStorageResourceSpecs.BLOODMAGIC_LP.getCodecId(),
                  QIOStorageDriveTier.BASE.getDefinition());
            array.updateQIODriveStack(0, new ItemStack(drive));
            FrequencyIdentity identity = new FrequencyIdentity("LP smoke", SecurityMode.PUBLIC, OWNER);
            for (TileEntityQIOComponent tile : new TileEntityQIOComponent[]{array, importer, exporter}) {
                tile.getSecurity().setOwnerUUID(OWNER);
                tile.setFrequency(FrequencyType.QIO, identity, OWNER);
            }
            frequency = array.getQIOFrequency();
            check(frequency != null, "LP frequency missing");
            check(frequency.massInsert(QIOStorageDescriptors.will(DemonWill.DEFAULT), 1, Action.SIMULATE) == 0,
                  "LP drive accepted Will");
            ItemStack orb = new ItemStack(RegistrarBloodMagicItems.BLOOD_ORB);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("orb", RegistrarBloodMagic.ORB_WEAK.getRegistryName().toString());
            tag.setTag("binding", new Binding(OWNER, "Alice").serializeNBT());
            orb.setTagCompound(tag);
            TileAltar altar = (TileAltar) world.getTileEntity(ALTAR);
            altar.setInventorySlotContents(0, orb);
            altar.checkTier();
            network = NetworkHelper.getSoulNetwork(OWNER);
            network.setCurrentEssence(500);
            check(BloodMagicLPTransferAdapter.INSTANCE.supports(altar, EnumFacing.WEST), "Native altar rejected bound orb");
        } else if (tick == 100) {
            check(stored() == 500 && network.getCurrentEssence() == 0,
                  "LP import failed: QIO=" + stored() + ", Soul Network=" + network.getCurrentEssence());
            checkOwnerHint();
            importer.clearFilters();
            importer.setImportWithoutFilter(false);
            exporter.setFilter(new QIOResourceFilter(QIOStorageDescriptors.lp(OWNER)));
        } else if (tick == 200) {
            check(stored() == 0 && network.getCurrentEssence() == 500,
                  "LP export failed: QIO=" + stored() + ", Soul Network=" + network.getCurrentEssence());
            exporter.clearFilters();
            exporter.setExportWithoutFilter(false);
            check(frequency.massInsert(QIOStorageDescriptors.lp(OWNER), 123, Action.EXECUTE) == 123,
                  "Cannot seed LP persistence check");
        } else if (tick == 220) {
            checkOwnerHint();
            return true;
        }
        return false;
    }

    private static long stored() {
        return frequency.getStored(QIOResourceTypeRegistry.INSTANCE.getOrTrack(QIOStorageDescriptors.lp(OWNER)));
    }

    private static void checkOwnerHint() {
        ItemStack stack = array.getQIODriveStacks().get(0);
        check(((ItemQIOStorageDrive) stack.getItem()).getSingleSoulNetwork(stack) != null,
              "Populated LP drive lost its owner hint");
        check(OWNER.equals(((ItemQIOStorageDrive) stack.getItem()).getSingleSoulNetwork(stack).getOwnerId()),
              "LP drive owner hint changed");
    }

    private static void place(WorldServer world, BlockPos pos, Block block) {
        world.setBlockToAir(pos);
        check(world.setBlockState(pos, block.getDefaultState()), "Cannot place LP smoke block at " + pos);
    }

    private static void inspectPlayer() throws Exception {
        String path = System.getenv("QIO_LP_DIAGNOSTIC_PLAYER");
        if (path == null) {
            return;
        }
        try (FileInputStream input = new FileInputStream(path)) {
            NBTTagList inventory = CompressedStreamTools.readCompressed(input).getTagList("Inventory", 10);
            for (int i = 0; i < inventory.tagCount(); i++) {
                NBTTagCompound entry = inventory.getCompoundTagAt(i);
                if (entry.getString("id").startsWith("bloodmagic:") ||
                      entry.getString("id").contains("bloodmagic_lp")) {
                    QIOStorage.LOGGER.info("LP diagnostic inventory: {}", entry);
                }
            }
        }
    }
}
