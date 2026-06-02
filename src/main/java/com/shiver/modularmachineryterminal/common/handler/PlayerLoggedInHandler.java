package com.shiver.modularmachineryterminal.common.handler;

import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import com.shiver.modularmachineryterminal.server.MachineLoadingList;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
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
        for (TileMultiblockMachineController controller : MachineLoadingList.loadedMachines(player)) {
            if (!controller.isStructureFormed()) {
                sendMachineInfo(player, controller);
            }
        }
    }

    public static void sendMachineInfo(EntityPlayerMP player, TileMultiblockMachineController controller) {

        BlockPos pos = controller.getPos();
        int dimension = controller.getWorld().provider.getDimension();

        TextComponentTranslation message = new TextComponentTranslation(
                "modular_machinery_terminal.machine_prefix",
                machineName(controller),
                dimension
        );
        TextComponentString cord = new TextComponentString(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        cord.setStyle(new Style().setColor(TextFormatting.AQUA));

        message.appendSibling(cord);
        message.appendSibling(new TextComponentTranslation("modular_machinery_terminal.unformed"));
        player.sendMessage(message);
    }

    private static String machineName(TileMultiblockMachineController controller) {
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null && controller instanceof TileMachineController) {
            machine = ((TileMachineController) controller).getParentMachine();
        }
        if (machine != null) {
            String localized = machine.getOriginalLocalizedName();
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }
            ResourceLocation rl = machine.getRegistryName();
            String key = rl.getNamespace() + "." + rl.getPath();
            if (I18n.canTranslate(key)) {
                return I18n.translateToLocal(key);
            }
            return rl.toString();
        }
        String formedName = controller.getFormedMachineName();
        return formedName == null || formedName.isEmpty() ? "Unknown Machine" : formedName;
    }

}
