package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineAccess;
import com.shiver.modularmachineryterminal.common.MachineKey;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who have remotely-opened containers via the terminal.
 * <p>
 * The actual distance check bypass is handled by Mixins:
 * <ul>
 *   <li>{@code MixinEntityPlayerMP} — bypasses vanilla's
 *       {@code Container.canInteractWith()} check</li>
 *   <li>{@code MixinMMCEEventHandler} — bypasses MMCE's
 *       {@code EventHandler.checkTERange()} check</li>
 * </ul>
 * <p>
 * The tick handler closes the remote GUI when the tracked controller or
 * target tile entity is no longer valid.
 */
public class RemoteContainerTracker {

    private static final Map<UUID, Session> tracked = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSync> pending = new ConcurrentHashMap<>();

    /**
     * Begin tracking a player for remote container access.
     *
     * @param player    the player who opened a remote container
     * @param machineKey the controller position of the source machine
     * @param targetPos   the position of the tile entity they are interacting with
     * @param syncContext client chunk restore context for this remote GUI
     */
    public static void prepare(EntityPlayerMP player, MachineKey machineKey, SyncContext syncContext) {
        if (player == null || machineKey == null || syncContext == null) {
            return;
        }
        PendingSync oldPending = pending.remove(player.getUniqueID());
        if (oldPending != null) {
            oldPending.restoreClientChunk(player);
        }
        pending.put(player.getUniqueID(), new PendingSync(machineKey, syncContext));
    }

    public static void cancelPending(EntityPlayerMP player, MachineKey machineKey) {
        if (player == null || machineKey == null) {
            return;
        }
        PendingSync pendingSync = pending.get(player.getUniqueID());
        if (pendingSync != null && pendingSync.machineKey.equals(machineKey)) {
            pending.remove(player.getUniqueID());
            pendingSync.restoreClientChunk(player);
        }
    }

    public static void track(EntityPlayerMP player, MachineKey machineKey, BlockPos targetPos) {
        if (player == null || machineKey == null || targetPos == null || player.openContainer == player.inventoryContainer) {
            return;
        }
        WorldServer world = player.server.getWorld(machineKey.dimension);
        if (world == null) {
            return;
        }
        TileEntity targetTile = world.getTileEntity(targetPos);
        if (targetTile == null) {
            return;
        }
        PendingSync pendingSync = pending.remove(player.getUniqueID());
        SyncContext syncContext = pendingSync != null && pendingSync.machineKey.equals(machineKey)
                ? pendingSync.syncContext
                : new SyncContext(player.dimension, targetPos.getX() >> 4, targetPos.getZ() >> 4, false);
        Session oldSession = tracked.remove(player.getUniqueID());
        if (oldSession != null) {
            oldSession.restoreClientChunk(player, syncContext);
        }
        tracked.put(player.getUniqueID(), new Session(machineKey, targetPos, targetTile.getClass(), player.openContainer, syncContext));
    }

    /**
     * Stop tracking a player.
     */
    public static void untrack(UUID playerId) {
        tracked.remove(playerId);
    }

    public static boolean isTrackedContainer(UUID playerId, Container container) {
        Session session = tracked.get(playerId);
        return session != null && session.container == container;
    }

    public static boolean isTrackedTarget(UUID playerId, TileEntity tile) {
        Session session = tracked.get(playerId);
        return session != null && tile != null
                && tile.getWorld() != null
                && session.machineKey.dimension == tile.getWorld().provider.getDimension()
                && session.targetPos.equals(tile.getPos());
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
        Session session = tracked.get(id);
        if (session == null) {
            return;
        }

        if (player.openContainer == player.inventoryContainer || player.openContainer != session.container) {
            untrack(player);
            return;
        }

        if (!session.isValid(player)) {
            player.closeScreen();
            untrack(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pending.remove(event.player.getUniqueID());
        if (event.player instanceof EntityPlayerMP) {
            untrack((EntityPlayerMP) event.player);
        } else {
            tracked.remove(event.player.getUniqueID());
        }
    }

    private static void untrack(EntityPlayerMP player) {
        Session session = tracked.remove(player.getUniqueID());
        if (session != null) {
            session.restoreClientChunk(player, null);
        }
    }

    private static class PendingSync {

        private final MachineKey machineKey;
        private final SyncContext syncContext;

        private PendingSync(MachineKey machineKey, SyncContext syncContext) {
            this.machineKey = machineKey;
            this.syncContext = syncContext;
        }

        private void restoreClientChunk(EntityPlayerMP player) {
            syncContext.restoreClientChunk(player, null);
        }
    }

    private static class Session {

        private final MachineKey machineKey;
        private final BlockPos targetPos;
        private final Class<?> targetType;
        private final Container container;
        private final SyncContext syncContext;

        private Session(MachineKey machineKey, BlockPos targetPos, Class<?> targetType, Container container, SyncContext syncContext) {
            this.machineKey = machineKey;
            this.targetPos = targetPos;
            this.targetType = targetType;
            this.container = container;
            this.syncContext = syncContext;
        }

        private boolean isValid(EntityPlayerMP player) {
            WorldServer world = player.server.getWorld(machineKey.dimension);
            if (world == null) {
                return false;
            }
            TileEntity controllerTile = world.getTileEntity(machineKey.pos);
            if (!(controllerTile instanceof TileMultiblockMachineController)) {
                return false;
            }
            TileEntity targetTile = world.getTileEntity(targetPos);
            if (targetTile == null || targetTile.getClass() != targetType) {
                return false;
            }
            TileMultiblockMachineController controller = (TileMultiblockMachineController) controllerTile;
            return PacketOpenMachineComponentGui.Handler.hasTarget(controller, targetPos)
                    && MachineAccess.canAccess(player, controller.getOwner(), true);
        }

        private void restoreClientChunk(EntityPlayerMP player, SyncContext replacement) {
            syncContext.restoreClientChunk(player, replacement);
        }
    }

    public static class SyncContext {

        private final int clientDimension;
        private final int chunkX;
        private final int chunkZ;
        private final boolean fakeChunkSent;

        public SyncContext(int clientDimension, int chunkX, int chunkZ, boolean fakeChunkSent) {
            this.clientDimension = clientDimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.fakeChunkSent = fakeChunkSent;
        }

        private void restoreClientChunk(EntityPlayerMP player, SyncContext replacement) {
            if (replacement != null
                    && clientDimension == replacement.clientDimension
                    && chunkX == replacement.chunkX
                    && chunkZ == replacement.chunkZ) {
                return;
            }
            if (player.dimension != clientDimension) {
                return;
            }
            WorldServer world = player.server.getWorld(clientDimension);
            if (world == null) {
                return;
            }
            boolean watchingNow = world.getPlayerChunkMap().isPlayerWatchingChunk(player, chunkX, chunkZ);
            if (watchingNow) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null) {
                    return;
                }
                player.connection.sendPacket(new SPacketChunkData(chunk, 65535));
                for (TileEntity tile : chunk.getTileEntityMap().values()) {
                    SPacketUpdateTileEntity packet = tile.getUpdatePacket();
                    if (packet != null) {
                        player.connection.sendPacket(packet);
                    } else {
                        player.connection.sendPacket(new SPacketUpdateTileEntity(tile.getPos(), 0, tile.getUpdateTag()));
                    }
                }
            } else if (fakeChunkSent) {
                player.connection.sendPacket(new SPacketUnloadChunk(chunkX, chunkZ));
            }
        }
    }
}
