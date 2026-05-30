package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.client.ClientTerminalData;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
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

    public PacketFullList() {
    }

    public PacketFullList(SummaryInfo summary, List<MachineInfo> machines) {
        this.summary = summary;
        this.machines.addAll(machines);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        summary = SummaryInfo.read(buffer);
        int count = buffer.readInt();
        machines.clear();
        for (int i = 0; i < count; i++) {
            machines.add(MachineInfo.read(buffer));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        summary.write(buffer);
        buffer.writeInt(machines.size());
        for (MachineInfo machine : machines) {
            machine.write(buffer);
        }
    }

    public static class Handler implements IMessageHandler<PacketFullList, IMessage> {

        @Override
        public IMessage onMessage(PacketFullList message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> ClientTerminalData.setFullList(message.summary, message.machines));
            return null;
        }
    }
}
