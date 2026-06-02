package com.shiver.modularmachineryterminal.common.handler;

import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.server.MachineCache;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber(modid = ModularMachineryTerminal.MOD_ID)
public class PlayerLoggedInHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        for (MachineInfo info : MachineCache.listUnformedMachines(player, true)) {
            sendMachineInfo(player, info);
        }
    }

    public static void sendMachineInfo(EntityPlayerMP player, MachineInfo info) {
        BlockPos pos = info.key.pos;
        int dimension = info.key.dimension;

        TextComponentTranslation message = new TextComponentTranslation(
                "modular_machinery_terminal.machine_prefix",
                info.name,
                dimension
        );
        TextComponentString cord = new TextComponentString(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        cord.setStyle(new Style().setColor(TextFormatting.AQUA));

        message.appendSibling(cord);
        message.appendSibling(new TextComponentTranslation("modular_machinery_terminal.unformed"));
        player.sendMessage(message);
    }
}
