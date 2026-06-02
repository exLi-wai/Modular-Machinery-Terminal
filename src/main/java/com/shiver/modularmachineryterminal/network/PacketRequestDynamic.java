package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.server.MachineCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketRequestDynamic implements IMessage {

    private final List<MachineKey> keys = new ArrayList<>();
    private boolean includeTeamControllers = true;

    /**
     * 创建 PacketRequestDynamic 实例。
     */
    public PacketRequestDynamic() {
    }

    /**
     * 创建 PacketRequestDynamic 实例。
     * @param keys 需要刷新的机器键列表
     */
    public PacketRequestDynamic(List<MachineKey> keys) {
        this.keys.addAll(keys);
    }

    /**
     * 创建 PacketRequestDynamic 实例。
     * @param keys 需要刷新的机器键列表
     * @param includeTeamControllers 是否包含团队成员拥有的控制器
     */
    public PacketRequestDynamic(List<MachineKey> keys, boolean includeTeamControllers) {
        this.keys.addAll(keys);
        this.includeTeamControllers = includeTeamControllers;
    }

    /**
     * 从网络缓冲区读取该消息的数据。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        keys.clear();
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            keys.add(MachineKey.read(buffer));
        }
        includeTeamControllers = buffer.readBoolean();
    }

    /**
     * 将该消息的数据写入网络缓冲区。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(keys.size());
        for (MachineKey key : keys) {
            key.write(buffer);
        }
        buffer.writeBoolean(includeTeamControllers);
    }

    public static class Handler implements IMessageHandler<PacketRequestDynamic, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketRequestDynamic message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TerminalNetwork.CHANNEL.sendTo(
                    MachineCache.createDynamicPacket(player, message.keys, message.includeTeamControllers),
                    player
            ));
            return null;
        }
    }
}
