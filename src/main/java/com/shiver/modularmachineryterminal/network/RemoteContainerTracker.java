package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineAccess;
import com.shiver.modularmachineryterminal.common.MachineKey;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
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

    /**
     * Begin tracking a player for remote container access.
     *
     * @param player    the player who opened a remote container
     * @param machineKey the controller position of the source machine
     * @param targetPos  the position of the tile entity they are interacting with
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
        tracked.put(player.getUniqueID(), new Session(machineKey, targetPos, targetTile.getClass(), player.openContainer));
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
            tracked.remove(id);
            return;
        }

        if (!session.isValid(player)) {
            player.closeScreen();
            tracked.remove(id);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        tracked.remove(event.player.getUniqueID());
    }

    private static class Session {

        private final MachineKey machineKey;
        private final BlockPos targetPos;
        private final Class<?> targetType;
        private final Container container;

        private Session(MachineKey machineKey, BlockPos targetPos, Class<?> targetType, Container container) {
            this.machineKey = machineKey;
            this.targetPos = targetPos;
            this.targetType = targetType;
            this.container = container;
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
    }
}
