package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.UUID;

public class FTBUtilitiesCompat {

    private static boolean searched;
    private static Method arePlayersInSameTeam;

    /**
     * 判断目标兼容模组是否已经加载。
     * @return 条件成立时返回 true，否则返回 false
     */
    public static boolean isLoaded() {
        return Loader.isModLoaded("ftbutilities") && Loader.isModLoaded("ftblib");
    }

    /**
     * 判断玩家与指定拥有者是否属于同一 FTB 队伍。
     * @param player 目标玩家
     * @param owner 机器拥有者 UUID
     * @return 条件成立时返回 true，否则返回 false
     */
    public static boolean isSameTeam(EntityPlayerMP player, UUID owner) {
        if (player == null || owner == null || !isLoaded()) {
            return false;
        }
        if (!init()) {
            return false;
        }

        try {
            return (Boolean) arePlayersInSameTeam.invoke(null, player.getUniqueID(), owner);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 执行模组初始化阶段的注册逻辑。
     * @return 条件成立时返回 true，否则返回 false
     */
    private static boolean init() {
        if (!searched) {
            searched = true;
            try {
                Class<?> api = Class.forName("com.feed_the_beast.ftblib.lib.data.FTBLibAPI");
                arePlayersInSameTeam = api.getMethod("arePlayersInSameTeam", UUID.class, UUID.class);
            } catch (Exception ignored) {
                arePlayersInSameTeam = null;
            }
        }
        return arePlayersInSameTeam != null;
    }
}
