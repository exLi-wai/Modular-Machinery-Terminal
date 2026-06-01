package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.MachineKey;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.tiles.TileFluidInputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileFluidOutputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PacketOpenMachineComponentGui implements IMessage {

    private MachineKey key;
    private ComponentGuiGroup group = ComponentGuiGroup.INPUT;
    private int index;
    private boolean prepared;

    public PacketOpenMachineComponentGui() {
    }

    public PacketOpenMachineComponentGui(MachineKey key, ComponentGuiGroup group, int index) {
        this(key, group, index, false);
    }

    public PacketOpenMachineComponentGui(MachineKey key, ComponentGuiGroup group, int index, boolean prepared) {
        this.key = key;
        this.group = group;
        this.index = index;
        this.prepared = prepared;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        key = MachineKey.read(buffer);
        group = ComponentGuiGroup.byId(buffer.readInt());
        index = buffer.readInt();
        prepared = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        key.write(buffer);
        buffer.writeInt(group.ordinal());
        buffer.writeInt(index);
        buffer.writeBoolean(prepared);
    }

    public static class Handler implements IMessageHandler<PacketOpenMachineComponentGui, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenMachineComponentGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> open(player, message));
            return null;
        }

        private static void open(EntityPlayerMP player, PacketOpenMachineComponentGui message) {
            if (message.key == null) {
                return;
            }
            WorldServer world = player.server.getWorld(message.key.dimension);
            if (world == null) {
                return;
            }
            TileEntity tile = world.getTileEntity(message.key.pos);
            if (!(tile instanceof TileMultiblockMachineController)) {
                player.sendMessage(new TextComponentString("Machine controller not found."));
                return;
            }
            TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
            UUID owner = controller.getOwner();
            if (owner != null && !owner.equals(player.getUniqueID())) {
                return;
            }
            List<TargetGui> targets = targets(controller, message.group);
            if (targets.isEmpty()) {
                player.sendMessage(new TextComponentString("No GUI found for " + message.group.name().toLowerCase() + "."));
                return;
            }
            int index = wrap(message.index, targets.size());
            TargetGui target = targets.get(index);
            // Sync the target block and tile entity to the client so that
            // Forge's client-side GUI handler can find the TileEntity.
            syncTargetToClient(player, world, target.pos);
            if (!message.prepared) {
                TerminalNetwork.CHANNEL.sendTo(new PacketPrepareComponentGui(message.key, message.group, index, targets.size(), target.pos), player);
                return;
            }
            TerminalNetwork.CHANNEL.sendTo(new PacketComponentGuiPager(message.key, message.group, index, targets.size(), target.pos), player);
            // Temporarily move the player to the target block so that
            // Container.canInteractWith distance checks pass for remote GUIs.
            double origX = player.posX;
            double origY = player.posY;
            double origZ = player.posZ;
            player.posX = target.pos.getX() + 0.5;
            player.posY = target.pos.getY() + 0.5;
            player.posZ = target.pos.getZ() + 0.5;
            try {
                player.openGui("modularmachinery", target.guiId, world, target.pos.getX(), target.pos.getY(), target.pos.getZ());
            } finally {
                player.posX = origX;
                player.posY = origY;
                player.posZ = origZ;
            }
            // Track this player for tick-based coordinate spoofing so that
            // canInteractWith distance checks pass on subsequent ticks.
            // Unlike the old RemoteContainerWrapper approach, this preserves
            // the original container class so that instanceof checks in
            // MMCE/AE2 packet handlers (e.g. PktMEInputBusInvAction,
            // PacketInventoryAction) still work correctly.
            RemoteContainerTracker.track(player, target.pos);
        }

        private static void syncTargetToClient(EntityPlayerMP player, WorldServer world, BlockPos pos) {
            if (player.dimension == world.provider.getDimension()) {
                // Same dimension: send full chunk data.
                Chunk chunk = world.getChunk(pos);
                player.connection.sendPacket(new SPacketChunkData(chunk, 65535));
            } else {
                // Cross-dimension: we cannot send the real chunk because
                // SPacketChunkData encodes sky light based on the source
                // world's hasSkyLight(), while the client decodes based on
                // its own world's hasSkyLight(). A mismatch corrupts the
                // entire buffer. SPacketBlockChange also fails because the
                // client may not have the target chunk loaded at all.
                //
                // Solution: build a minimal fake chunk belonging to the
                // PLAYER's world (so sky light matches) that contains only
                // the target block and its tile entity data.
                WorldServer playerWorld = player.getServerWorld();
                Chunk fakeChunk = new Chunk(playerWorld, pos.getX() >> 4, pos.getZ() >> 4);
                IBlockState targetState = world.getBlockState(pos);
                int sectionIndex = pos.getY() >> 4;
                ExtendedBlockStorage[] storage = fakeChunk.getBlockStorageArray();
                storage[sectionIndex] = new ExtendedBlockStorage(
                        sectionIndex << 4, playerWorld.provider.hasSkyLight());
                storage[sectionIndex].set(
                        pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, targetState);
                // Include the TE in the chunk so handleChunkData creates it
                // on the client and populates its data via handleUpdateTag.
                TileEntity targetTe = world.getTileEntity(pos);
                if (targetTe != null) {
                    fakeChunk.getTileEntityMap().put(pos, targetTe);
                }
                player.connection.sendPacket(new SPacketChunkData(fakeChunk, 65535));
            }
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null) {
                SPacketUpdateTileEntity packet = tile.getUpdatePacket();
                if (packet != null) {
                    player.connection.sendPacket(packet);
                } else {
                    player.connection.sendPacket(new SPacketUpdateTileEntity(pos, 0, tile.getUpdateTag()));
                }
            }
        }

        private static List<TargetGui> targets(TileMultiblockMachineController controller, ComponentGuiGroup group) {
            Map<BlockPos, TargetGui> targets = new LinkedHashMap<>();
            Map<TileEntity, ProcessingComponent<?>> components = controller.getGeneralComponents();
            if (components != null) {
                for (TileEntity tile : components.keySet()) {
                    addTarget(targets, tile, group);
                }
            }
            if (group == ComponentGuiGroup.UPGRADE && controller.getFoundUpgradeBuses() != null) {
                for (TileUpgradeBus.UpgradeBusProvider provider : controller.getFoundUpgradeBuses()) {
                    addTarget(targets, upgradeBusTile(provider), group);
                }
            }
            List<TargetGui> list = new ArrayList<>(targets.values());
            list.sort(Comparator
                    .comparingInt((TargetGui target) -> target.pos.getY())
                    .thenComparingInt(target -> target.pos.getX())
                    .thenComparingInt(target -> target.pos.getZ()));
            return list;
        }

        private static void addTarget(Map<BlockPos, TargetGui> targets, TileEntity tile, ComponentGuiGroup group) {
            int guiId = guiId(tile, group);
            if (tile != null && guiId >= 0) {
                targets.put(tile.getPos(), new TargetGui(tile.getPos(), guiId));
            }
        }

        private static int guiId(TileEntity tile, ComponentGuiGroup group) {
            if (group == ComponentGuiGroup.INPUT) {
                if (tile instanceof TileItemInputBus) return guiId("BUS_INVENTORY");
                if (tile instanceof TileFluidInputHatch) return guiId("TANK_INVENTORY");
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEItemInputBus")) return guiId("ME_ITEM_INPUT_BUS");
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEFluidInputBus")) return guiId("ME_FLUID_INPUT_BUS");
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEGasInputBus")) return guiId("ME_GAS_INPUT_BUS");
            } else if (group == ComponentGuiGroup.OUTPUT) {
                if (tile instanceof TileItemOutputBus) return guiId("BUS_INVENTORY");
                if (tile instanceof TileFluidOutputHatch) return guiId("TANK_INVENTORY");
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEItemOutputBus")) return guiId("ME_ITEM_OUTPUT_BUS");
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEFluidOutputBus")) return guiId("ME_FLUID_OUTPUT_BUS");
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEGasOutputBus")) return guiId("ME_GAS_OUTPUT_BUS");
            } else if (group == ComponentGuiGroup.PATTERN) {
                if (isType(tile, "github.kasuminova.mmce.common.tile.MEPatternProvider")) return guiId("ME_PATTERN_PROVIDER");
            } else if (group == ComponentGuiGroup.UPGRADE) {
                if (tile instanceof TileUpgradeBus) return guiId("UPGRADE_BUS");
            }
            return -1;
        }

        private static boolean isType(Object object, String className) {
            if (object == null) {
                return false;
            }
            return isType(object.getClass(), className);
        }

        private static boolean isType(Class<?> type, String className) {
            while (type != null) {
                if (className.equals(type.getName()) || hasInterface(type, className)) {
                    return true;
                }
                type = type.getSuperclass();
            }
            return false;
        }

        private static boolean hasInterface(Class<?> type, String className) {
            for (Class<?> iface : type.getInterfaces()) {
                if (className.equals(iface.getName()) || hasInterface(iface, className)) {
                    return true;
                }
            }
            return false;
        }

        private static int guiId(String name) {
            try {
                Class<?> type = Class.forName("hellfirepvp.modularmachinery.common.CommonProxy$GuiType");
                Object value = Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), name);
                return ((Enum<?>) value).ordinal();
            } catch (Exception ignored) {
                return -1;
            }
        }

        private static TileUpgradeBus upgradeBusTile(TileUpgradeBus.UpgradeBusProvider provider) {
            try {
                Field parent = provider.getClass().getDeclaredField("parent");
                parent.setAccessible(true);
                Object tile = parent.get(provider);
                return tile instanceof TileUpgradeBus ? (TileUpgradeBus) tile : null;
            } catch (Exception ignored) {
                return null;
            }
        }

        private static int wrap(int index, int size) {
            int result = index % size;
            return result < 0 ? result + size : result;
        }
    }

    private static class TargetGui {
        private final BlockPos pos;
        private final int guiId;

        private TargetGui(BlockPos pos, int guiId) {
            this.pos = pos;
            this.guiId = guiId;
        }
    }
}
