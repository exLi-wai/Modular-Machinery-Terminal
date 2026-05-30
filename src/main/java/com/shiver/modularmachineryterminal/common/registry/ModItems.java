package com.shiver.modularmachineryterminal.common.registry;

import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import com.shiver.modularmachineryterminal.common.item.ItemTerminal;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ModularMachineryTerminal.MOD_ID)
public final class ModItems {

    public static ItemTerminal TERMINAL;

    private ModItems() {
    }

    public static void init() {
        TERMINAL = new ItemTerminal();
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(TERMINAL);
    }

    public static CreativeTabs tab() {
        return CreativeTabs.MISC;
    }
}
