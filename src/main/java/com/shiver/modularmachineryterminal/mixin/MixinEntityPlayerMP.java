package com.shiver.modularmachineryterminal.mixin;

import com.shiver.modularmachineryterminal.network.RemoteContainerTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

    @Redirect(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z"
            )
    )
    /**
     * 重定向容器交互检查以允许被追踪的远程组件容器继续使用。
     * @param container 目标容器
     * @param player 目标玩家
     * @return 条件成立时返回 true，否则返回 false
     */
    private boolean terminal$redirectCanInteractWith(Container container, EntityPlayer player) {
        if (RemoteContainerTracker.isTrackedContainer(player.getUniqueID(), container)) {
            return true;
        }
        return container.canInteractWith(player);
    }
}
