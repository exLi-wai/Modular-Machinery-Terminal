package com.shiver.modularmachineryterminal.client.gui;

import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.network.PacketOpenMachineComponentGui;
import com.shiver.modularmachineryterminal.network.TerminalNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.lang.reflect.Field;

public class ComponentGuiPager {

    private static final int PREV_ID = 9801;
    private static final int NEXT_ID = 9802;

    private static MachineKey key;
    private static ComponentGuiGroup group;
    private static int index;
    private static int total;
    private static BlockPos targetPos;
    private static boolean openedFromTerminal = false;

    /**
     * 设置当前客户端组件 GUI 翻页上下文。
     * @param newKey 新的机器键
     * @param newGroup 新的组件 GUI 分组
     * @param newIndex 新的页索引
     * @param newTotal 新的总页数
     * @param newTargetPos 新的目标组件位置
     */
    public static void set(MachineKey newKey, ComponentGuiGroup newGroup, int newIndex, int newTotal, BlockPos newTargetPos) {
        key = newKey;
        group = newGroup;
        index = newIndex;
        total = newTotal;
        targetPos = newTargetPos;
        openedFromTerminal = true;
    }

    /**
     * 清空当前客户端组件 GUI 翻页上下文。
     */
    public static void clear() {

        key = null;
        group = null;
        index = 0;
        total = 0;
        targetPos = null;
        openedFromTerminal = false;
    }

    /**
     * 在 GUI 打开事件中维护远程组件 GUI 上下文。
     * @param event 触发该逻辑的事件对象
     */
    @SubscribeEvent
    public void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
        if (openedFromTerminal) {
            if (event.getGui() == null) {
                event.setGui(new GuiTerminal());
                clear();
            } else if (!(event.getGui() instanceof GuiContainer)) {
                clear();
            }
        } else {
            clear();
        }
    }

    /**
     * 在 GUI 初始化后注入组件翻页按钮。
     * @param event 触发该逻辑的事件对象
     */
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!active(event.getGui())) {
            return;
        }
        GuiContainer gui = (GuiContainer) event.getGui();
        int guiTop = getField(gui, "guiTop", "field_147009_r", event.getGui().height / 2 - 83);
        
        String text = I18n.format("gui.modular_machinery_terminal.component_page", index + 1, total);
        if (targetPos != null) {
            text += " (" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + ")";
        }
        Minecraft mc = Minecraft.getMinecraft();
        int textWidth = mc.fontRenderer.getStringWidth(text);
        int centerX = event.getGui().width / 2;
        int y = guiTop - 22;
        
        event.getButtonList().add(new GuiButton(PREV_ID, centerX - textWidth / 2 - 26, y, 20, 20, "<"));
        event.getButtonList().add(new GuiButton(NEXT_ID, centerX + textWidth / 2 + 6, y, 20, 20, ">"));
    }

    /**
     * 处理组件 GUI 翻页按钮点击。
     * @param event 触发该逻辑的事件对象
     * @throws IOException 读写输入或输出失败时抛出
     */
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

    /**
     * 在组件 GUI 上绘制当前页码。
     * @param event 触发该逻辑的事件对象
     */
    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!active(event.getGui())) {
            return;
        }
        GuiContainer gui = (GuiContainer) event.getGui();
        int guiTop = getField(gui, "guiTop", "field_147009_r", event.getGui().height / 2 - 83);
        String text = I18n.format("gui.modular_machinery_terminal.component_page", index + 1, total);
        if (targetPos != null) {
            text += " (" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + ")";
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawStringWithShadow(text, event.getGui().width / 2 - mc.fontRenderer.getStringWidth(text) / 2, guiTop - 16, 0xFFFFFF);
    }

    /**
     * 判断当前 GUI 是否处于终端组件翻页上下文中。
     * @param gui 目标 GUI 对象
     * @return 条件成立时返回 true，否则返回 false
     */
    private static boolean active(Object gui) {
        if (key == null || group == null || total <= 1 || !(gui instanceof GuiContainer)) {
            return false;
        }
        String name = gui.getClass().getName();
        return name.startsWith("hellfirepvp.modularmachinery.client.gui.")
                || name.startsWith("github.kasuminova.mmce.client.gui.")
                || name.startsWith("kport.modularmagic.client.gui.");
    }

    /**
     * 通过反射读取 GUI 布局字段。
     * @param gui 目标 GUI 对象
     * @param name 目标名称
     * @param obfName obfName 参数
     * @param def def 参数
     * @return 计算得到的数值
     */
    private static int getField(GuiContainer gui, String name, String obfName, int def) {
        try {
            Class<?> c = GuiContainer.class;
            Field f = null;
            try {
                f = c.getDeclaredField(name);
            } catch (Exception e) {
                try {
                    f = c.getDeclaredField(obfName);
                } catch (Exception ex) {
                    return def;
                }
            }
            f.setAccessible(true);
            return f.getInt(gui);
        } catch (Exception e) {
            return def;
        }
    }
}
