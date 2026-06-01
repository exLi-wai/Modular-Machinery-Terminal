package com.shiver.modularmachineryterminal.common;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class MachineAccess {

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
