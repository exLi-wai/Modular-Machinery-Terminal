package com.shiver.modularmachineryterminal.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who have remotely-opened containers via the terminal.
 * <p>
 * This is a simple registry that maps player UUIDs to the target block
 * position. The actual distance check bypass is handled by Mixins:
 * <ul>
 *   <li>{@code MixinEntityPlayerMP} — bypasses vanilla's
 *       {@code Container.canInteractWith()} check</li>
 *   <li>{@code MixinMMCEEventHandler} — bypasses MMCE's
 *       {@code EventHandler.checkTERange()} check</li>
 * </ul>
 * <p>
 * The tick handler only detects when the container is closed so the
 * player can be automatically untracked.
 */
public class RemoteContainerTracker {

    /** Maps player UUID → target block position. */
    private static final Map<UUID, BlockPos> tracked = new ConcurrentHashMap<>();

    /**
     * Begin tracking a player for remote container access.
     *
     * @param player    the player who opened a remote container
     * @param targetPos the position of the tile entity they are interacting with
     */
    public static void track(EntityPlayerMP player, BlockPos targetPos) {
        tracked.put(player.getUniqueID(), targetPos);
    }

    /**
     * Stop tracking a player.
     */
    public static void untrack(UUID playerId) {
        tracked.remove(playerId);
    }

    /**
     * Check whether a player is currently being tracked for remote access.
     * Called by the Mixins to decide whether to bypass distance checks.
     */
    public static boolean isTracked(UUID playerId) {
        return tracked.containsKey(playerId);
    }

    /**
     * Detects when a tracked player closes their container and
     * automatically removes them from the tracker.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.SERVER) {
            return;
        }
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        if (!tracked.containsKey(id)) {
            return;
        }

        // If the player has closed the container, stop tracking.
        if (player.openContainer == player.inventoryContainer) {
            tracked.remove(id);
        }
    }
}
