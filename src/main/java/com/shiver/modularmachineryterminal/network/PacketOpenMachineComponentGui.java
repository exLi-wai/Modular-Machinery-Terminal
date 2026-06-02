package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.MachineAccess;
import com.shiver.modularmachineryterminal.common.MachineKey;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.tiles.TileFluidInputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileFluidOutputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
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

    /**
     * 创建 PacketOpenMachineComponentGui 实例。
     */
    public PacketOpenMachineComponentGui() {
    }

    /**
     * 创建 PacketOpenMachineComponentGui 实例。
     * @param key 目标机器键
     * @param group 组件 GUI 分组
     * @param index 目标索引
     */
    public PacketOpenMachineComponentGui(MachineKey key, ComponentGuiGroup group, int index) {
        this(key, group, index, false);
    }

    /**
     * 创建 PacketOpenMachineComponentGui 实例。
     * @param key 目标机器键
     * @param group 组件 GUI 分组
     * @param index 目标索引
     * @param prepared 客户端是否已经完成预同步
     */
    public PacketOpenMachineComponentGui(MachineKey key, ComponentGuiGroup group, int index, boolean prepared) {
        this.key = key;
        this.group = group;
        this.index = index;
        this.prepared = prepared;
    }

    /**
     * 从网络缓冲区读取该消息的数据。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        key = MachineKey.read(buffer);
        group = ComponentGuiGroup.byId(buffer.readInt());
        index = buffer.readInt();
        prepared = buffer.readBoolean();
    }

    /**
     * 将该消息的数据写入网络缓冲区。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        key.write(buffer);
        buffer.writeInt(group.ordinal());
        buffer.writeInt(index);
        buffer.writeBoolean(prepared);
    }

    public static class Handler implements IMessageHandler<PacketOpenMachineComponentGui, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketOpenMachineComponentGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> open(player, message));
            return null;
        }

        /**
         * 校验机器和目标组件后打开对应远程 GUI。
         * @param player 目标玩家
         * @param message 收到的网络消息
         */
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
                player.sendMessage(new TextComponentTranslation("message.modular_machinery_terminal.machine_controller_not_found"));
                return;
            }
            TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
            UUID owner = controller.getOwner();
            if (!MachineAccess.canAccess(player, owner, true)) {
                return;
            }
            List<TargetGui> targets = targets(controller, message.group);
            if (targets.isEmpty()) {
                player.sendMessage(new TextComponentTranslation(
                        "message.modular_machinery_terminal.no_gui_found",
                        new TextComponentTranslation(groupTranslationKey(message.group))));
                return;
            }
            int index = wrap(message.index, targets.size());
            TargetGui target = targets.get(index);
            if (!message.prepared) {

                RemoteContainerTracker.SyncContext syncContext = syncTargetToClient(player, world, target.pos);
                RemoteContainerTracker.prepare(player, message.key, syncContext);
                TerminalNetwork.CHANNEL.sendTo(new PacketPrepareComponentGui(message.key, message.group, index, targets.size(), target.pos), player);
                return;
            }
            TerminalNetwork.CHANNEL.sendTo(new PacketComponentGuiPager(message.key, message.group, index, targets.size(), target.pos), player);

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

            

            
            RemoteContainerTracker.track(player, message.key, target.pos);
        }

        /**
         * 把远程目标方块和方块实体同步到客户端。
         * @param player 目标玩家
         * @param world 目标世界
         * @param pos 目标方块位置
         * @return 方法执行结果
         */
        private static RemoteContainerTracker.SyncContext syncTargetToClient(EntityPlayerMP player, WorldServer world, BlockPos pos) {
            WorldServer playerWorld = player.getServerWorld();
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            boolean watching = playerWorld.getPlayerChunkMap().isPlayerWatchingChunk(player, chunkX, chunkZ);
            boolean fakeChunkSent = false;

            if (player.dimension == world.provider.getDimension() && watching) {
                player.connection.sendPacket(new SPacketBlockChange(world, pos));
            } else {
                Chunk fakeChunk = new Chunk(playerWorld, chunkX, chunkZ);
                IBlockState targetState = world.getBlockState(pos);
                int sectionIndex = pos.getY() >> 4;
                ExtendedBlockStorage[] storage = fakeChunk.getBlockStorageArray();
                storage[sectionIndex] = new ExtendedBlockStorage(
                        sectionIndex << 4, playerWorld.provider.hasSkyLight());
                storage[sectionIndex].set(
                        pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, targetState);
                TileEntity targetTe = world.getTileEntity(pos);
                if (targetTe != null) {
                    fakeChunk.getTileEntityMap().put(pos, targetTe);
                }
                player.connection.sendPacket(new SPacketChunkData(fakeChunk, 65535));
                fakeChunkSent = true;
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
            return new RemoteContainerTracker.SyncContext(player.dimension, chunkX, chunkZ, fakeChunkSent);
        }

        /**
         * 收集指定机器中属于目标分组的可打开 GUI。
         * @param controller 目标机器控制器
         * @param group 组件 GUI 分组
         * @return 符合条件的列表
         */
        private static List<TargetGui> targets(TileMultiblockMachineController controller, ComponentGuiGroup group) {
            Map<BlockPos, TargetGui> targets = new LinkedHashMap<>();
            addTarget(targets, controller, group);
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
            if (group == ComponentGuiGroup.SMART_INTERFACE && controller.getFoundSmartInterfaces() != null) {
                for (TileSmartInterface.SmartInterfaceProvider provider : controller.getFoundSmartInterfaces().keySet()) {
                    addTarget(targets, smartInterfaceTile(provider), group);
                }
            }
            List<TargetGui> list = new ArrayList<>(targets.values());
            list.sort(Comparator
                    .comparingInt((TargetGui target) -> target.pos.getY())
                    .thenComparingInt(target -> target.pos.getX())
                    .thenComparingInt(target -> target.pos.getZ()));
            return list;
        }

        /**
         * 判断机器是否包含指定位置的可打开组件 GUI。
         * @param controller 目标机器控制器
         * @param pos 目标方块位置
         * @return 条件成立时返回 true，否则返回 false
         */
        static boolean hasTarget(TileMultiblockMachineController controller, BlockPos pos) {
            if (controller == null || pos == null) {
                return false;
            }
            for (ComponentGuiGroup group : ComponentGuiGroup.values()) {
                for (TargetGui target : targets(controller, group)) {
                    if (pos.equals(target.pos)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 把可打开 GUI 的方块实体加入目标集合。
         * @param targets 目标 GUI 集合
         * @param tile 目标方块实体
         * @param group 组件 GUI 分组
         */
        private static void addTarget(Map<BlockPos, TargetGui> targets, TileEntity tile, ComponentGuiGroup group) {
            int guiId = guiId(tile, group);
            if (tile != null && guiId >= 0) {
                targets.put(tile.getPos(), new TargetGui(tile.getPos(), guiId));
            }
        }

        /**
         * 解析组件方块实体或枚举名称对应的 Modular Machinery GUI 编号。
         * @param tile 目标方块实体
         * @param group 组件 GUI 分组
         * @return 计算得到的数值
         */
        private static int guiId(TileEntity tile, ComponentGuiGroup group) {
            if (group == ComponentGuiGroup.CONTROLLER) {
                if (isType(tile, "hellfirepvp.modularmachinery.common.tiles.TileFactoryController")) return guiId("FACTORY");
                if (isType(tile, "hellfirepvp.modularmachinery.common.tiles.TileMachineController")) return guiId("CONTROLLER");
            } else if (group == ComponentGuiGroup.INPUT) {
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
            } else if (group == ComponentGuiGroup.SMART_INTERFACE) {
                if (tile instanceof TileSmartInterface) return guiId("SMART_INTERFACE");
            }
            return -1;
        }

        /**
         * 判断对象或类型是否匹配指定类名或接口名。
         * @param object 待检查对象
         * @param className 待匹配的完整类名
         * @return 条件成立时返回 true，否则返回 false
         */
        private static boolean isType(Object object, String className) {
            if (object == null) {
                return false;
            }
            return isType(object.getClass(), className);
        }

        /**
         * 判断对象或类型是否匹配指定类名或接口名。
         * @param type 待检查的类型
         * @param className 待匹配的完整类名
         * @return 条件成立时返回 true，否则返回 false
         */
        private static boolean isType(Class<?> type, String className) {
            while (type != null) {
                if (className.equals(type.getName()) || hasInterface(type, className)) {
                    return true;
                }
                type = type.getSuperclass();
            }
            return false;
        }

        /**
         * 递归判断类型是否实现指定接口。
         * @param type 待检查的类型
         * @param className 待匹配的完整类名
         * @return 条件成立时返回 true，否则返回 false
         */
        private static boolean hasInterface(Class<?> type, String className) {
            for (Class<?> iface : type.getInterfaces()) {
                if (className.equals(iface.getName()) || hasInterface(iface, className)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 解析组件方块实体或枚举名称对应的 Modular Machinery GUI 编号。
         * @param name 目标名称
         * @return 计算得到的数值
         */
        private static int guiId(String name) {
            try {
                Class<?> type = Class.forName("hellfirepvp.modularmachinery.common.CommonProxy$GuiType");
                Object value = Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), name);
                return ((Enum<?>) value).ordinal();
            } catch (Exception ignored) {
                return -1;
            }
        }

        /**
         * 返回组件 GUI 分组对应的本地化键。
         * @param group 组件 GUI 分组
         * @return 对应的文本
         */
        private static String groupTranslationKey(ComponentGuiGroup group) {
            return "message.modular_machinery_terminal.gui_group." + group.name().toLowerCase();
        }

        /**
         * 从升级总线提供器中取出实际方块实体。
         * @param provider 组件提供器
         * @return 方法执行结果
         */
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

        /**
         * 从智能接口提供器中取出实际方块实体。
         * @param provider 组件提供器
         * @return 方法执行结果
         */
        private static TileSmartInterface smartInterfaceTile(TileSmartInterface.SmartInterfaceProvider provider) {
            try {
                Field parent = provider.getClass().getDeclaredField("parent");
                parent.setAccessible(true);
                Object tile = parent.get(provider);
                return tile instanceof TileSmartInterface ? (TileSmartInterface) tile : null;
            } catch (Exception ignored) {
                return null;
            }
        }

        /**
         * 把索引环绕到指定列表大小范围内。
         * @param index 目标索引
         * @param size 列表大小
         * @return 计算得到的数值
         */
        private static int wrap(int index, int size) {
            int result = index % size;
            return result < 0 ? result + size : result;
        }
    }

    private static class TargetGui {
        private final BlockPos pos;
        private final int guiId;

        /**
         * 创建 TargetGui 实例。
         * @param pos 目标方块位置
         * @param guiId guiId 参数
         */
        private TargetGui(BlockPos pos, int guiId) {
            this.pos = pos;
            this.guiId = guiId;
        }
    }
}
