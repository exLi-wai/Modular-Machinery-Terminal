package com.shiver.modularmachineryterminal.client;

import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import com.shiver.modularmachineryterminal.common.BaublesCompat;
import com.shiver.modularmachineryterminal.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * 按键处理器，负责处理终端快捷键。
 */
public class KeyHandler {

    /** 打开终端的按键绑定，默认 Shift + X */
    private static final KeyBinding OPEN_TERMINAL = new KeyBinding(
            "key.modularmachinery_terminal.open_terminal",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            Keyboard.KEY_X,
            "key.categories.modularmachinery_terminal"
    );

    /**
     * 创建 KeyHandler 实例并注册按键绑定。
     */
    public KeyHandler() {
        ClientRegistry.registerKeyBinding(OPEN_TERMINAL);
    }

    /**
     * 处理按键输入事件。
     * @param event 按键输入事件
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || !OPEN_TERMINAL.isPressed()) {
            return;
        }
        if (mc.world != null && mc.player != null && hasTerminalInInventory(mc.player)) {
            ModularMachineryTerminal.proxy.openTerminalGui();
        }
    }

    /**
     * 检查玩家背包或饰品栏中是否有终端物品。
     * @param player 目标玩家
     * @return 如果背包或饰品栏中有终端物品返回 true，否则返回 false
     */
    private boolean hasTerminalInInventory(EntityPlayer player) {
        // 检查普通背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ModItems.TERMINAL) {
                return true;
            }
        }

        // 检查饰品栏（如果 Baubles 已加载）
        if (BaublesCompat.isLoaded()) {
            int slot = BaublesCompat.isBaubleEquipped(player, ModItems.TERMINAL);
            if (slot >= 0) {
                return true;
            }
        }

        return false;
    }
}
