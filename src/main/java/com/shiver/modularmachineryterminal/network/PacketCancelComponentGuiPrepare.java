package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCancelComponentGuiPrepare implements IMessage {

    private MachineKey key;

    /**
     * 创建 PacketCancelComponentGuiPrepare 实例。
     */
    public PacketCancelComponentGuiPrepare() {
    }

    /**
     * 创建 PacketCancelComponentGuiPrepare 实例。
     * @param key 目标机器键
     */
    public PacketCancelComponentGuiPrepare(MachineKey key) {
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

    public static class Handler implements IMessageHandler<PacketCancelComponentGuiPrepare, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketCancelComponentGuiPrepare message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> RemoteContainerTracker.cancelPending(player, message.key));
            return null;
        }
    }
}
