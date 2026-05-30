package com.shiver.modularmachineryterminal.common.item;

import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import com.shiver.modularmachineryterminal.common.registry.ModItems;
import com.shiver.modularmachineryterminal.network.PacketRequestFullList;
import com.shiver.modularmachineryterminal.network.TerminalNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemTerminal extends Item {

    public ItemTerminal() {
        setRegistryName(ModularMachineryTerminal.MOD_ID, "terminal");
        setTranslationKey(ModularMachineryTerminal.MOD_ID + ".terminal");
        setCreativeTab(ModItems.tab());
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            ModularMachineryTerminal.proxy.openTerminalGui();
            TerminalNetwork.CHANNEL.sendToServer(new PacketRequestFullList());
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
}
