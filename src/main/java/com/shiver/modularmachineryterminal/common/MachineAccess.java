package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class MachineAccess {

    /**
     * 判断玩家是否可以访问指定拥有者的机器。
     * @param player 目标玩家
     * @param owner 机器拥有者 UUID
     * @param includeTeam includeTeam 参数
     * @return 条件成立时返回 true，否则返回 false
     */
    public static boolean canAccess(EntityPlayerMP player, UUID owner, boolean includeTeam) {
        if (owner == null) {
            return true;
        }
        if (player == null) {
            return false;
        }
        if (owner.equals(player.getUniqueID())) {
            return true;
        }
        return includeTeam
                && TerminalConfig.teamAccessEnabled
                && FTBUtilitiesCompat.isSameTeam(player, owner);
    }
}
