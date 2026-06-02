package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.client.ClientTerminalData;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
import com.shiver.modularmachineryterminal.common.TerminalConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketFullList implements IMessage {

    private SummaryInfo summary = new SummaryInfo();
    private final List<MachineInfo> machines = new ArrayList<>();
    private boolean teleportEnabled = true;
    private String teleportRequiredGameStage = "";
    private boolean teamAccessEnabled = true;

    /**
     * 创建 PacketFullList 实例。
     */
    public PacketFullList() {
    }

    /**
     * 创建 PacketFullList 实例。
     * @param summary 汇总信息
     * @param machines 机器列表
     */
    public PacketFullList(SummaryInfo summary, List<MachineInfo> machines) {
        this.summary = summary;
        this.machines.addAll(machines);
        this.teleportEnabled = TerminalConfig.teleportEnabled;
        this.teleportRequiredGameStage = TerminalConfig.teleportRequiredGameStage;
        this.teamAccessEnabled = TerminalConfig.teamAccessEnabled;
    }

    /**
     * 从网络缓冲区读取该消息的数据。
     * @param buf 网络字节缓冲区
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        summary = SummaryInfo.read(buffer);
        int count = buffer.readInt();
        machines.clear();
        for (int i = 0; i < count; i++) {
            machines.add(MachineInfo.read(buffer));
        }
        teleportEnabled = buffer.readBoolean();
        teleportRequiredGameStage = buffer.readString(Short.MAX_VALUE);
        teamAccessEnabled = buffer.readBoolean();
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
        buffer.writeBoolean(teleportEnabled);
        buffer.writeString(teleportRequiredGameStage);
        buffer.writeBoolean(teamAccessEnabled);
    }

    public static class Handler implements IMessageHandler<PacketFullList, IMessage> {

        /**
         * 处理收到的网络消息，并把实际逻辑切换到对应线程执行。
         * @param message 收到的网络消息
         * @param ctx 网络消息上下文
         * @return 需要回复的网络消息，通常为 null
         */
        @Override
        public IMessage onMessage(PacketFullList message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TerminalConfig.updateClientConfig(message.teleportEnabled, message.teleportRequiredGameStage, message.teamAccessEnabled);
                ClientTerminalData.setFullList(message.summary, message.machines);
            });
            return null;
        }
    }
}
