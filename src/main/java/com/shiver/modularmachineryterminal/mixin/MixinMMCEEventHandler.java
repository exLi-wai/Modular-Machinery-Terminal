package com.shiver.modularmachineryterminal.mixin;

import com.shiver.modularmachineryterminal.network.RemoteContainerTracker;
import github.kasuminova.mmce.common.handler.EventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EventHandler.class, remap = false)
public abstract class MixinMMCEEventHandler {

    @Redirect(
            method = "onPlayerTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lgithub/kasuminova/mmce/common/handler/EventHandler;checkTERange(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/tileentity/TileEntity;)Z"
            )
    )
    /**
     * 重定向机器组件距离检查以允许终端打开的远程组件 GUI 保持开启。
     * @param player 目标玩家
     * @param te te 参数
     * @return 条件成立时返回 true，否则返回 false
     */
    private static boolean terminal$redirectCheckTERange(EntityPlayer player, TileEntity te) {
        if (RemoteContainerTracker.isTrackedTarget(player.getUniqueID(), te)) {
            return false; 
        }

        BlockPos tePos = te.getPos();
        BlockPos playerPos = player.getPosition();
        return tePos.getDistance(playerPos.getX(), playerPos.getY(), playerPos.getZ()) >= 6.0D;
    }
}
