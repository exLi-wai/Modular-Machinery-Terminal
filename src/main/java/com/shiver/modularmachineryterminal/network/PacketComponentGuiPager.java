package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.client.gui.ComponentGuiPager;
import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.MachineKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketComponentGuiPager implements IMessage {

    private MachineKey key;
    private ComponentGuiGroup group = ComponentGuiGroup.INPUT;
    private int index;
    private int total;
    private BlockPos targetPos;

    /**
     * 创建 PacketComponentGuiPager 实例。
     */
    public PacketComponentGuiPager() {
    }

    /**
     * 创建 PacketComponentGuiPager 实例。
     * @param key 目标机器键
     * @param group 组件 GUI 分组
     * @param index 目标索引
     * @param total 总数量
     * @param targetPos 目标组件位置
     */
    public PacketComponentGuiPager(MachineKey key, ComponentGuiGroup group, int index, int total, BlockPos targetPos) {
        this.key = key;
        this.group = group;
        this.index = index;
        this.total = total;
        this.targetPos = targetPos;
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
        total = buffer.readInt();
        targetPos = buffer.readBlockPos();
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
        buffer.writeInt(total);
        buffer.writeBlockPos(targetPos);
    }

    public static class Handler implements IMessageHandler<PacketComponentGuiPager, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketComponentGuiPager message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> ComponentGuiPager.set(message.key, message.group, message.index, message.total, message.targetPos));
            return null;
        }
    }
}
