package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * Baubles 模组兼容层，处理饰品栏相关功能。
 */
public class BaublesCompat {

    private static boolean searched;
    private static Method isBaubleEquipped;

    /**
     * 判断 Baubles 模组是否已加载。
     * @return 如果 Baubles 已加载返回 true，否则返回 false
     */
    public static boolean isLoaded() {
        return Loader.isModLoaded("baubles");
    }

    /**
     * 检查玩家饰品栏中是否有指定物品。
     * @param player 目标玩家
     * @param bauble 目标物品
     * @return 如果找到返回槽位索引，否则返回 -1
     */
    public static int isBaubleEquipped(EntityPlayer player, Item bauble) {
        if (player == null || bauble == null || !isLoaded()) {
            return -1;
        }
        if (!init()) {
            return -1;
        }

        try {
            return (Integer) isBaubleEquipped.invoke(null, player, bauble);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * 初始化反射方法。
     * @return 如果初始化成功返回 true，否则返回 false
     */
    private static boolean init() {
        if (!searched) {
            searched = true;
            try {
                Class<?> api = Class.forName("baubles.api.BaublesApi");
                isBaubleEquipped = api.getMethod("isBaubleEquipped", EntityPlayer.class, Item.class);
            } catch (Exception ignored) {
                isBaubleEquipped = null;
            }
        }
        return isBaubleEquipped != null;
    }
}
