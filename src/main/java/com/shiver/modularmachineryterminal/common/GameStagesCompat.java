package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

public class GameStagesCompat {

    private static Method hasStageMethod;
    private static boolean searched;

    /**
     * 判断玩家是否拥有指定 GameStages 阶段。
     * @param player 目标玩家
     * @param stage stage 参数
     * @return 条件成立时返回 true，否则返回 false
     */
    public static boolean hasStage(EntityPlayer player, String stage) {
        if (player == null || stage == null || stage.trim().isEmpty()) {
            return true;
        }
        if (!Loader.isModLoaded("gamestages")) {
            return true;
        }

        Method method = hasStageMethod();
        if (method == null) {
            return true;
        }

        try {
            return (Boolean) method.invoke(null, player, stage.trim());
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 执行 has stage method 相关逻辑。
     * @return 方法执行结果
     */
    private static Method hasStageMethod() {
        if (!searched) {
            searched = true;
            try {
                Class<?> helper = Class.forName("net.darkhax.gamestages.GameStageHelper");
                hasStageMethod = helper.getMethod("hasStage", EntityPlayer.class, String.class);
            } catch (Exception ignored) {
                hasStageMethod = null;
            }
        }
        return hasStageMethod;
    }
}
