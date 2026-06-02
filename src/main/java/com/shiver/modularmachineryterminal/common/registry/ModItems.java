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

    /**
     * 创建 ModItems 实例。
     */
    private ModItems() {
    }

    /**
     * 执行模组初始化阶段的注册逻辑。
     */
    public static void init() {
        TERMINAL = new ItemTerminal();
    }

    /**
     * 在 Forge 物品注册事件中注册本模组物品。
     * @param event 触发该逻辑的事件对象
     */
    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(TERMINAL);
    }

    /**
     * 返回终端物品所属的创造模式标签。
     * @return 方法执行结果
     */
    public static CreativeTabs tab() {
        return CreativeTabs.MISC;
    }
}
