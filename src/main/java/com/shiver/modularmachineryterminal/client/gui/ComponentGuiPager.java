package com.shiver.modularmachineryterminal.client.gui;

import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.network.PacketOpenMachineComponentGui;
import com.shiver.modularmachineryterminal.network.TerminalNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;

public class ComponentGuiPager {

    private static final int PREV_ID = 9801;
    private static final int NEXT_ID = 9802;

    private static MachineKey key;
    private static ComponentGuiGroup group;
    private static int index;
    private static int total;

    public static void set(MachineKey newKey, ComponentGuiGroup newGroup, int newIndex, int newTotal) {
        key = newKey;
        group = newGroup;
        index = newIndex;
        total = newTotal;
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!active(event.getGui())) {
            return;
        }
        int x = event.getGui().width / 2;
        int y = event.getGui().height / 2 - 96;
        event.getButtonList().add(new GuiButton(PREV_ID, x - 112, y, 18, 18, "<"));
        event.getButtonList().add(new GuiButton(NEXT_ID, x + 94, y, 18, 18, ">"));
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) throws IOException {
        if (!active(event.getGui())) {
            return;
        }
        if (event.getButton().id == PREV_ID) {
            TerminalNetwork.CHANNEL.sendToServer(new PacketOpenMachineComponentGui(key, group, index - 1));
            event.setCanceled(true);
        } else if (event.getButton().id == NEXT_ID) {
            TerminalNetwork.CHANNEL.sendToServer(new PacketOpenMachineComponentGui(key, group, index + 1));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!active(event.getGui())) {
            return;
        }
        String text = I18n.format("gui.modular_machinery_terminal.component_page", index + 1, total);
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawStringWithShadow(text, event.getGui().width / 2 - mc.fontRenderer.getStringWidth(text) / 2, event.getGui().height / 2 - 92, 0xFFFFFF);
    }

    private static boolean active(Object gui) {
        if (key == null || group == null || total <= 1 || !(gui instanceof GuiContainer)) {
            return false;
        }
        String name = gui.getClass().getName();
        return name.startsWith("hellfirepvp.modularmachinery.client.gui.")
                || name.startsWith("github.kasuminova.mmce.client.gui.")
                || name.startsWith("kport.modularmagic.client.gui.");
    }
}
