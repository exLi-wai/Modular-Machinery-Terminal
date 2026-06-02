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

public class RemoteContainerTracker {

    private static final Map<UUID, Session> tracked = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSync> pending = new ConcurrentHashMap<>();

    /**
     * 记录远程组件 GUI 打开前的客户端同步上下文。
     * @param player 目标玩家
     * @param machineKey 目标机器键
     * @param syncContext 客户端同步上下文
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

    /**
     * 取消玩家尚未完成的远程组件 GUI 准备状态。
     * @param player 目标玩家
     * @param machineKey 目标机器键
     */
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

    /**
     * 开始追踪玩家当前打开的远程组件容器。
     * @param player 目标玩家
     * @param machineKey 目标机器键
     * @param targetPos 目标组件位置
     */
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
     * 停止追踪指定玩家的远程容器状态。
     * @param playerId 玩家 UUID
     */
    public static void untrack(UUID playerId) {
        tracked.remove(playerId);
    }

    /**
     * 判断容器是否为当前追踪的远程组件容器。
     * @param playerId 玩家 UUID
     * @param container 目标容器
     * @return 条件成立时返回 true，否则返回 false
     */
    public static boolean isTrackedContainer(UUID playerId, Container container) {
        Session session = tracked.get(playerId);
        return session != null && session.container == container;
    }

    /**
     * 判断方块实体是否为当前追踪的远程组件目标。
     * @param playerId 玩家 UUID
     * @param tile 目标方块实体
     * @return 条件成立时返回 true，否则返回 false
     */
    public static boolean isTrackedTarget(UUID playerId, TileEntity tile) {
        Session session = tracked.get(playerId);
        return session != null && tile != null
                && tile.getWorld() != null
                && session.machineKey.dimension == tile.getWorld().provider.getDimension()
                && session.targetPos.equals(tile.getPos());
    }

    /**
     * 在玩家 tick 中临时修正位置以通过远程容器距离检查。
     * @param event 触发该逻辑的事件对象
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

    /**
     * 在玩家退出时清理远程容器追踪状态。
     * @param event 触发该逻辑的事件对象
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pending.remove(event.player.getUniqueID());
        if (event.player instanceof EntityPlayerMP) {
            untrack((EntityPlayerMP) event.player);
        } else {
            tracked.remove(event.player.getUniqueID());
        }
    }

    /**
     * 停止追踪指定玩家的远程容器状态。
     * @param player 目标玩家
     */
    private static void untrack(EntityPlayerMP player) {
        Session session = tracked.remove(player.getUniqueID());
        if (session != null) {
            session.restoreClientChunk(player, null);
        }
    }

    private static class PendingSync {

        private final MachineKey machineKey;
        private final SyncContext syncContext;

        /**
         * 创建 PendingSync 实例。
         * @param machineKey 目标机器键
         * @param syncContext 客户端同步上下文
         */
        private PendingSync(MachineKey machineKey, SyncContext syncContext) {
            this.machineKey = machineKey;
            this.syncContext = syncContext;
        }

        /**
         * 按同步上下文恢复客户端区块显示状态。
         * @param player 目标玩家
         */
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

        /**
         * 创建 Session 实例。
         * @param machineKey 目标机器键
         * @param targetPos 目标组件位置
         * @param targetType targetType 参数
         * @param container 目标容器
         * @param syncContext 客户端同步上下文
         */
        private Session(MachineKey machineKey, BlockPos targetPos, Class<?> targetType, Container container, SyncContext syncContext) {
            this.machineKey = machineKey;
            this.targetPos = targetPos;
            this.targetType = targetType;
            this.container = container;
            this.syncContext = syncContext;
        }

        /**
         * 判断远程容器会话是否仍然有效。
         * @param player 目标玩家
         * @return 条件成立时返回 true，否则返回 false
         */
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

        /**
         * 按同步上下文恢复客户端区块显示状态。
         * @param player 目标玩家
         * @param replacement 替换用同步上下文
         */
        private void restoreClientChunk(EntityPlayerMP player, SyncContext replacement) {
            syncContext.restoreClientChunk(player, replacement);
        }
    }

    public static class SyncContext {

        private final int clientDimension;
        private final int chunkX;
        private final int chunkZ;
        private final boolean fakeChunkSent;

        /**
         * 创建 SyncContext 实例。
         * @param clientDimension 客户端维度 ID
         * @param chunkX 区块 X 坐标
         * @param chunkZ 区块 Z 坐标
         * @param fakeChunkSent 是否已发送伪区块
         */
        public SyncContext(int clientDimension, int chunkX, int chunkZ, boolean fakeChunkSent) {
            this.clientDimension = clientDimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.fakeChunkSent = fakeChunkSent;
        }

        /**
         * 按同步上下文恢复客户端区块显示状态。
         * @param player 目标玩家
         * @param replacement 替换用同步上下文
         */
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
