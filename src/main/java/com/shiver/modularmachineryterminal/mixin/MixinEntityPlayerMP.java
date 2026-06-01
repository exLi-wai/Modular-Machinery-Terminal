package com.shiver.modularmachineryterminal.mixin;

import com.shiver.modularmachineryterminal.network.RemoteContainerTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin into {@link EntityPlayerMP#onUpdate()} to bypass the
 * {@code Container.canInteractWith()} distance check for players
 * who have remotely-opened containers via the terminal.
 * <p>
 * Without this, the GUI would be closed every tick because the player
 * is too far from the target tile entity.
 */
@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

    /**
     * Redirects the {@code this.openContainer.canInteractWith(this)} call
     * in {@code EntityPlayerMP.onUpdate()} (line ~363). If the player is
     * tracked by {@link RemoteContainerTracker}, returns {@code true}
     * unconditionally to prevent the GUI from being closed.
     */
    @Redirect(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z"
            )
    )
    private boolean terminal$redirectCanInteractWith(Container container, EntityPlayer player) {
        if (RemoteContainerTracker.isTracked(player.getUniqueID())) {
            return true;
        }
        return container.canInteractWith(player);
    }
}
