package com.shiver.modularmachineryterminal.server.command;

import com.shiver.modularmachineryterminal.common.handler.PlayerLoggedInHandler;
import com.shiver.modularmachineryterminal.server.MachineList;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;
import java.util.List;

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

        EntityPlayerMP player = args.length == 1 ? getPlayer(server, sender, args[0]) : getCommandSenderAsPlayer(sender);
        List<TileMultiblockMachineController> controllers = MachineList.loadedMachines(player);
        for (TileMultiblockMachineController controller : controllers) {
            if (!controller.isStructureFormed()) {
                PlayerLoggedInHandler.sendMachineInfo(player, controller);
            }
        }
    }

}
