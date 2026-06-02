package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.client.ClientTerminalData;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketDynamic implements IMessage {

    private SummaryInfo summary = new SummaryInfo();
    private final List<MachineInfo> machines = new ArrayList<>();
    private final List<MachineKey> removed = new ArrayList<>();

    /**
     * 创建 PacketDynamic 实例。
     */
    public PacketDynamic() {
    }

    /**
     * 创建 PacketDynamic 实例。
     * @param summary 汇总信息
     * @param machines 机器列表
     * @param removed 已移除机器键列表
     */
    public PacketDynamic(SummaryInfo summary, List<MachineInfo> machines, List<MachineKey> removed) {
        this.summary = summary;
        this.machines.addAll(machines);
        this.removed.addAll(removed);
    }

    /**
     * 从网络缓冲区读取该消息的数据。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        summary = SummaryInfo.read(buffer);
        machines.clear();
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            machines.add(MachineInfo.read(buffer));
        }
        removed.clear();
        int removedCount = buffer.readInt();
        for (int i = 0; i < removedCount; i++) {
            removed.add(MachineKey.read(buffer));
        }
    }

    /**
     * 将该消息的数据写入网络缓冲区。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        summary.write(buffer);
        buffer.writeInt(machines.size());
        for (MachineInfo machine : machines) {
            machine.write(buffer);
        }
        buffer.writeInt(removed.size());
        for (MachineKey key : removed) {
            key.write(buffer);
        }
    }

    public static class Handler implements IMessageHandler<PacketDynamic, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketDynamic message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> ClientTerminalData.updateDynamic(message.summary, message.machines, message.removed));
            return null;
        }
    }
}
