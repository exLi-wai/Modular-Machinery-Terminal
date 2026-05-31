package com.shiver.modularmachineryterminal.client;

import com.shiver.modularmachineryterminal.client.gui.GuiTerminal;
import com.shiver.modularmachineryterminal.client.gui.ComponentGuiPager;
import com.shiver.modularmachineryterminal.common.CommonProxy;
import com.shiver.modularmachineryterminal.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.model.ModelLoader;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        ModelLoader.setCustomModelResourceLocation(
                ModItems.TERMINAL,
                0,
                new ModelResourceLocation(ModItems.TERMINAL.getRegistryName(), "inventory")
        );
        MinecraftForge.EVENT_BUS.register(new ComponentGuiPager());
    }

    @Override
    public void openTerminalGui() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiTerminal());
    }
}
