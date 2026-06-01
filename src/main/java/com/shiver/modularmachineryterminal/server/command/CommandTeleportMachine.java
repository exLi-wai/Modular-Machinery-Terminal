package com.shiver.modularmachineryterminal.server.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandTeleportMachine extends CommandBase {

    @Override
    @Nonnull
    public String getName() {
        return "mmt_tp";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/mmt_tp <player> <dimension> <x> <y> <z>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean checkPermission(@Nonnull MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(getRequiredPermissionLevel(), "tp");
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 5) {
            throw new WrongUsageException(getUsage(sender));
        }

        EntityPlayerMP player = getPlayer(server, sender, args[0]);
        int dimension = parseInt(args[1]);
        BlockPos pos = parseBlockPos(sender, args, 2, false);
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            throw new CommandException("commands.forge.setdim.invalid.dim", dimension);
        }

        if (player.dimension == dimension) {
            player.connection.setPlayerLocation(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.rotationYaw, player.rotationPitch);
            notifyCommandListener(sender, this, "commands.tp.success.coordinates", player.getName(), pos.getX(), pos.getY(), pos.getZ());
            return;
        }

        player.changeDimension(dimension, new MachineTeleporter(pos));
        notifyCommandListener(sender, this, "commands.tp.success.coordinates", player.getName(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length > 2 && args.length <= 5) {
            return getTabCompletionCoordinate(args, 2, targetPos);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    private static class MachineTeleporter implements ITeleporter {

        private final BlockPos pos;

        private MachineTeleporter(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public void placeEntity(World world, net.minecraft.entity.Entity entity, float yaw) {
            entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, entity.rotationPitch);
        }
    }
}
