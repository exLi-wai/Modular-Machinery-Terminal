package com.shiver.modularmachineryterminal.mixin;

import com.shiver.modularmachineryterminal.network.RemoteContainerTracker;
import github.kasuminova.mmce.common.handler.EventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin into MMCE's {@link EventHandler} to bypass the range check
 * in {@code onPlayerTick()} for players who have remotely-opened
 * containers via the terminal.
 * <p>
 * Without this, MMCE's EventHandler would call {@code player.closeScreen()}
 * when the player is ≥6 blocks away from the tile entity.
 * <p>
 * Uses {@code remap = false} because MMCE's classes are not obfuscated.
 */
@Mixin(value = EventHandler.class, remap = false)
public abstract class MixinMMCEEventHandler {

    /**
     * Redirects the {@code checkTERange(player, te)} call in
     * {@code onPlayerTick()}. If the player is tracked by
     * {@link RemoteContainerTracker}, returns {@code false}
     * (meaning "not out of range") to prevent GUI closure.
     */
    @Redirect(
            method = "onPlayerTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lgithub/kasuminova/mmce/common/handler/EventHandler;checkTERange(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/tileentity/TileEntity;)Z"
            )
    )
    private static boolean terminal$redirectCheckTERange(EntityPlayer player, TileEntity te) {
        if (RemoteContainerTracker.isTracked(player.getUniqueID())) {
            return false; // false = not out of range → don't close GUI
        }
        // Call original logic inline (since checkTERange is private static,
        // we replicate its simple logic here).
        BlockPos tePos = te.getPos();
        BlockPos playerPos = player.getPosition();
        return tePos.getDistance(playerPos.getX(), playerPos.getY(), playerPos.getZ()) >= 6.0D;
    }
}
