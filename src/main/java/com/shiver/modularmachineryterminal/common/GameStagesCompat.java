package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

public class GameStagesCompat {

    private static Method hasStageMethod;
    private static boolean searched;

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
