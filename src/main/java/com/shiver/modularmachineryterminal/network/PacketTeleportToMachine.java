package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.GameStagesCompat;
import com.shiver.modularmachineryterminal.common.MachineAccess;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.TerminalConfig;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class PacketTeleportToMachine implements IMessage {

    private MachineKey key;

    /**
     * 创建 PacketTeleportToMachine 实例。
     */
    public PacketTeleportToMachine() {
    }

    /**
     * 创建 PacketTeleportToMachine 实例。
     * @param key 目标机器键
     */
    public PacketTeleportToMachine(MachineKey key) {
        this.key = key;
    }

    /**
     * 从网络缓冲区读取该消息的数据。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        key = MachineKey.read(new PacketBuffer(buf));
    }

    /**
     * 将该消息的数据写入网络缓冲区。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void toBytes(ByteBuf buf) {
        key.write(new PacketBuffer(buf));
    }

    public static class Handler implements IMessageHandler<PacketTeleportToMachine, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketTeleportToMachine message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> teleport(player, message.key));
            return null;
        }

        /**
         * 执行 teleport 相关逻辑。
         * @param player 目标玩家
         * @param key 目标机器键
         */
        private static void teleport(EntityPlayerMP player, MachineKey key) {
            MinecraftServer server = player.getServer();
            if (server == null || key == null) {
                return;
            }
            if (!TerminalConfig.teleportEnabled) {
                return;
            }
            String stage = TerminalConfig.teleportRequiredGameStage;
            if (stage != null && !stage.isEmpty() && !GameStagesCompat.hasStage(player, stage)) {
                return;
            }
            WorldServer world = server.getWorld(key.dimension);
            if (world == null) {
                player.sendMessage(new TextComponentTranslation("message.modular_machinery_terminal.dimension_not_found", key.dimension));
                return;
            }
            TileEntity tile = world.getTileEntity(key.pos);
            if (!(tile instanceof TileMultiblockMachineController)) {
                player.sendMessage(new TextComponentTranslation("message.modular_machinery_terminal.machine_controller_not_found"));
                return;
            }
            TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
            UUID owner = controller.getOwner();
            if (!MachineAccess.canAccess(player, owner, true)) {
                return;
            }
            EnumFacing facing = controller.getControllerRotation();
            if (facing == null || facing.getAxis().isVertical()) {
                facing = EnumFacing.SOUTH;
            }
            BlockPos target = key.pos.offset(facing);
            double x = target.getX() + 0.5D;
            int y = target.getY();
            double z = target.getZ() + 0.5D;
            String command;
            if (player.dimension == key.dimension) {
                command = "tp " + player.getName() + " " + x + " " + y + " " + z;
            } else {
                command = "forge setdimension " + player.getName() + " " + key.dimension + " " + x + " " + y + " " + z;
            }
            server.getCommandManager().executeCommand(server, command);
        }
    }
}
