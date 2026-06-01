package com.shiver.modularmachineryterminal.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who have remotely-opened containers and temporarily spoofs
 * their coordinates to the target block position during each server tick.
 * <p>
 * This replaces the old {@code RemoteContainerWrapper} approach which broke
 * {@code instanceof} checks in MMCE/AE2 packet handlers (e.g.
 * {@code PktMEInputBusInvAction}, AE2's {@code PacketInventoryAction}).
 * <p>
 * By spoofing coordinates instead of wrapping the container, the original
 * container class is preserved and all {@code instanceof} checks work correctly.
 * <p>
 * Timing:
 * <ul>
 *   <li>{@code PlayerTickEvent.START} at {@code HIGHEST} priority: save real
 *       coordinates, spoof to target position (before MMCE's EventHandler
 *       distance check and before {@code onUpdateEntity}/{@code canInteractWith})
 *   <li>{@code PlayerTickEvent.END} at {@code LOWEST} priority: restore real
 *       coordinates (after all processing is done)
 * </ul>
 */
public class RemoteContainerTracker {

    /** Maps player UUID → target block position for coordinate spoofing. */
    private static final Map<UUID, BlockPos> tracked = new ConcurrentHashMap<>();

    /** Maps player UUID → saved real coordinates {posX, posY, posZ}. */
    private static final Map<UUID, double[]> savedCoords = new ConcurrentHashMap<>();

    /**
     * Begin tracking a player for remote container coordinate spoofing.
     *
     * @param player    the player who opened a remote container
     * @param targetPos the position of the tile entity they are interacting with
     */
    public static void track(EntityPlayerMP player, BlockPos targetPos) {
        tracked.put(player.getUniqueID(), targetPos);
    }

    /**
     * Stop tracking a player (e.g. when the container is closed).
     */
    public static void untrack(UUID playerId) {
        tracked.remove(playerId);
        savedCoords.remove(playerId);
    }

    /**
     * Check whether a player is currently being tracked.
     */
    public static boolean isTracked(UUID playerId) {
        return tracked.containsKey(playerId);
    }

    /**
     * At the START of each server tick, spoof tracked players' coordinates
     * to their target block position. This must run BEFORE:
     * <ul>
     *   <li>MMCE's {@code EventHandler.onPlayerTick} (which checks range
     *       and closes the GUI if too far away)</li>
     *   <li>Vanilla's {@code EntityPlayerMP.onUpdateEntity()} (which calls
     *       {@code Container.canInteractWith()} for distance checks)</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.SERVER) {
            return;
        }
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        BlockPos target = tracked.get(id);
        if (target == null) {
            return;
        }

        // If the player has closed the container, stop tracking.
        if (player.openContainer == player.inventoryContainer) {
            untrack(id);
            return;
        }

        // Save real coordinates and spoof to target position.
        savedCoords.put(id, new double[]{player.posX, player.posY, player.posZ});
        player.posX = target.getX() + 0.5;
        player.posY = target.getY() + 0.5;
        player.posZ = target.getZ() + 0.5;
    }

    /**
     * At the END of each server tick, restore the real coordinates for all
     * tracked players. Uses {@code LOWEST} priority to run after all other
     * tick handlers have finished.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerTickEnd(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != Side.SERVER) {
            return;
        }
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        double[] coords = savedCoords.remove(id);
        if (coords == null) {
            return;
        }

        // Restore real coordinates.
        player.posX = coords[0];
        player.posY = coords[1];
        player.posZ = coords[2];
    }
}
