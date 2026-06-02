package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.server.MachineCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestFullList implements IMessage {

    private boolean includeTeamControllers = true;

    /**
     * 创建 PacketRequestFullList 实例。
     */
    public PacketRequestFullList() {
    }

    /**
     * 创建 PacketRequestFullList 实例。
     * @param includeTeamControllers 是否包含团队成员拥有的控制器
     */
    public PacketRequestFullList(boolean includeTeamControllers) {
        this.includeTeamControllers = includeTeamControllers;
    }

    /**
     * 从网络缓冲区读取该消息的数据。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        includeTeamControllers = buf.readBoolean();
    }

    /**
     * 将该消息的数据写入网络缓冲区。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(includeTeamControllers);
    }

    public static class Handler implements IMessageHandler<PacketRequestFullList, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketRequestFullList message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TerminalNetwork.CHANNEL.sendTo(
                    MachineCache.createFullListPacket(player, message.includeTeamControllers),
                    player
            ));
            return null;
        }
    }
}
