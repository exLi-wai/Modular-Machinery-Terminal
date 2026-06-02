package com.shiver.modularmachineryterminal.server.command;

import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.handler.PlayerLoggedInHandler;
import com.shiver.modularmachineryterminal.server.MachineCache;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;

public class CommandMachineStatus extends CommandBase {

    @Override
    @Nonnull
    public String getName() {
        return "mmt_machines";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/mmt_machines [player]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 1) {
            throw new CommandException(getUsage(sender));
        }

        EntityPlayerMP queryTarget;
        if (args.length == 1) {
            if (!sender.canUseCommand(2, getName())) {
                throw new CommandException("commands.generic.permission");
            }
            queryTarget = getPlayer(server, sender, args[0]);
        } else {
            queryTarget = getCommandSenderAsPlayer(sender);
        }
        for (MachineInfo info : MachineCache.listUnformedMachines(queryTarget, true)) {
            PlayerLoggedInHandler.sendMachineInfo(sender, info);
        }
    }
}
