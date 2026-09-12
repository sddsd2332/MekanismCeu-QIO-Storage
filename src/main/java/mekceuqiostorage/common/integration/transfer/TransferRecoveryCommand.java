package mekceuqiostorage.common.integration.transfer;

import mekanism.common.tile.qio.TileEntityQIOComponent;
import mekceuqiostorage.common.QIOStorage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

/** Operator reconciliation of recorded outcomes, without repeating the native resource operation. */
public final class TransferRecoveryCommand extends CommandBase {
    @Override public String getName() { return "qiostorage-recovery"; }
    @Override public int getRequiredPermissionLevel() { return 4; }
    @Override public String getUsage(ICommandSender sender) {
        return "/qiostorage-recovery list <dimension> | settle <dimension> <id> <verifiedActual> <qioX> <qioY> <qioZ>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new WrongUsageException(getUsage(sender));
        WorldServer world = server.getWorld(parseInt(args[1]));
        if (world == null) throw new CommandException("Recovery dimension is not loaded");
        TransferRecovery data = TransferRecovery.get(world);
        if ("list".equals(args[0]) && args.length == 2) {
            for (NBTTagCompound record : data.list()) {
                if (!record.getBoolean("complete")) sender.sendMessage(new TextComponentString(record.toString()));
            }
            sender.sendMessage(new TextComponentString("Unknown outcomes require evidence from the original operation; " +
                  "a later balance or an attempted rollback is not proof."));
            return;
        }
        if (!"settle".equals(args[0]) || args.length != 7) throw new WrongUsageException(getUsage(sender));
        BlockPos pos = parseBlockPos(sender, args, 4, false);
        if (!world.isBlockLoaded(pos)) throw new CommandException("QIO destination chunk is not loaded");
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityQIOComponent)) throw new CommandException("Destination is not a QIO component");
        long actual = parseLong(args[3]);
        try {
            long remaining = data.recover(args[2], actual, ((TileEntityQIOComponent) tile).getQIOFrequency());
            QIOStorage.LOGGER.warn("{} reconciled QIO transfer {} using actual={}, destination={}, remaining={}",
                  sender.getName(), args[2], actual, pos, remaining);
            sender.sendMessage(new TextComponentString("Recovery " + args[2] + ": remaining=" + remaining));
        } catch (IllegalArgumentException failure) {
            throw new CommandException(failure.getMessage());
        }
    }
}
