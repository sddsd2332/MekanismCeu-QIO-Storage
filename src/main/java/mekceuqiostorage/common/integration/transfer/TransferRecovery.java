package mekceuqiostorage.common.integration.transfer;

import mekanism.api.Action;
import mekanism.api.qio.resource.QIOResourceDescriptor;
import mekanism.common.content.qio.QIOFrequency;
import mekceuqiostorage.common.QIOStorage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent, non-spendable transfer claims. Unknown native outcomes require independent evidence. */
public final class TransferRecovery extends WorldSavedData {
    private static final String NAME = QIOStorage.MODID + "_transfer_recovery";
    private static final String TILE_KEY = QIOStorage.MODID + ":transfer_recovery";
    private final Map<String, NBTTagCompound> records = new LinkedHashMap<>();

    public TransferRecovery() { super(NAME); }
    public TransferRecovery(String name) { super(name); }

    public static TransferRecovery get(World world) {
        if (world == null || world.isRemote) return null;
        MapStorage storage = world.getPerWorldStorage();
        if (storage == null) return null;
        TransferRecovery data = (TransferRecovery) storage.getOrLoadData(TransferRecovery.class, NAME);
        if (data == null) {
            data = new TransferRecovery();
            storage.setData(NAME, data);
        }
        return data;
    }

    public static boolean isBlocked(TileEntity target) {
        if (target == null) return true;
        NBTTagCompound local = target.getTileData().getCompoundTag(TILE_KEY);
        if (local.isEmpty()) return false;
        TransferRecovery data = get(target.getWorld());
        NBTTagCompound record = data == null ? local : data.records.get(local.getString("id"));
        return record == null || !record.getBoolean("complete");
    }

    public static NBTTagCompound pending(TileEntity target) {
        NBTTagCompound local = target.getTileData().getCompoundTag(TILE_KEY);
        TransferRecovery data = get(target.getWorld());
        NBTTagCompound record = data == null ? local : data.records.get(local.getString("id"));
        return record == null ? local.copy() : record.copy();
    }

    static long hold(TileEntity target, EnumFacing face, QIOResourceDescriptor descriptor,
          long outerRequest, boolean insertion, UncertainTransferException failure) {
        long reserved = insertion ? outerRequest : 0;
        NBTTagCompound record = new NBTTagCompound();
        record.setString("id", UUID.randomUUID().toString());
        record.setTag("resource", descriptor.write());
        record.setString("tileClass", target.getClass().getName());
        record.setLong("position", target.getPos().toLong());
        record.setInteger("dimension", target.getWorld() == null ? 0 : target.getWorld().provider.getDimension());
        record.setInteger("face", face.getIndex());
        record.setBoolean("insertion", insertion);
        record.setLong("requested", outerRequest);
        record.setLong("nativeRequested", failure.requested);
        record.setLong("settled", reserved);
        record.setString("reason", String.valueOf(failure.getCause()));
        if (failure.before != null) record.setDouble("before", failure.before);
        if (failure.after != null) record.setDouble("after", failure.after);
        if (failure.actual != null) confirm(record, failure.actual);
        target.getTileData().setTag(TILE_KEY, record.copy());
        TransferRecovery data = get(target.getWorld());
        if (data != null) {
            data.records.put(record.getString("id"), record);
            data.markDirty();
        }
        try {
            target.markDirty();
        } catch (LinkageError | RuntimeException ignored) { }
        QIOStorage.LOGGER.error("QIO native transfer quarantined: {}. Use /qiostorage-recovery list; " +
              "do not retry or infer the old operation from a later balance.", record);
        return reserved;
    }

    private static void confirm(NBTTagCompound record, long actual) {
        record.setBoolean("known", true);
        record.setLong("actual", actual);
        long remaining = record.getBoolean("insertion") ?
              Math.subtractExact(record.getLong("settled"), actual) :
              Math.subtractExact(actual, record.getLong("settled"));
        record.setLong("remaining", remaining);
    }

    /** Evidence must describe the original call; a later native balance is not such evidence. */
    public long recover(String id, long actual, QIOFrequency frequency) {
        NBTTagCompound record = records.get(id);
        if (record == null) throw new IllegalArgumentException("Unknown recovery id");
        if (record.getBoolean("complete")) return 0;
        if (record.getBoolean("known")) {
            if (record.getLong("actual") != actual) throw new IllegalArgumentException("Conflicting transfer evidence");
        } else {
            if (actual < 0 || actual > record.getLong("nativeRequested")) {
                throw new IllegalArgumentException("Unconfirmed outcome must be within the native request");
            }
            confirm(record, actual);
            markDirty();
        }
        long remaining = record.getLong("remaining");
        if (remaining != 0) {
            if (frequency == null || !frequency.isValid() || frequency.isRemoved()) {
                throw new IllegalArgumentException("A live QIO frequency is required for settlement");
            }
            QIOResourceDescriptor resource = QIOResourceDescriptor.read(record.getCompoundTag("resource"));
            if (!resource.isResolved()) throw new IllegalArgumentException("Resource codec is unavailable");
            long moved = remaining > 0 ? frequency.massInsert(resource, remaining, Action.EXECUTE) :
                  frequency.massExtract(resource, Math.negateExact(remaining), Action.EXECUTE);
            remaining = remaining > 0 ? remaining - moved : remaining + moved;
            record.setLong("remaining", remaining);
        }
        record.setBoolean("complete", remaining == 0);
        markDirty();
        return remaining;
    }

    public List<NBTTagCompound> list() {
        List<NBTTagCompound> result = new ArrayList<>();
        for (NBTTagCompound record : records.values()) result.add(record.copy());
        return Collections.unmodifiableList(result);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        records.clear();
        NBTTagList list = nbt.getTagList("records", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound record = list.getCompoundTagAt(i).copy();
            records.put(record.getString("id"), record);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (NBTTagCompound record : records.values()) list.appendTag(record.copy());
        nbt.setTag("records", list);
        return nbt;
    }
}
