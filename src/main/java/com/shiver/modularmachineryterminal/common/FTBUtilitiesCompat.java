package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.UUID;

public class FTBUtilitiesCompat {

    private static boolean searched;
    private static Method arePlayersInSameTeam;

    public static boolean isLoaded() {
        return Loader.isModLoaded("ftbutilities") && Loader.isModLoaded("ftblib");
    }

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
