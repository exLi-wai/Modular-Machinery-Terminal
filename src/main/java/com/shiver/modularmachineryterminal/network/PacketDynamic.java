package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.client.ClientTerminalData;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketDynamic implements IMessage {

    private final List<MachineInfo> machines = new ArrayList<>();

    public PacketDynamic() {
    }

    public PacketDynamic(List<MachineInfo> machines) {
        this.machines.addAll(machines);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        machines.clear();
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            machines.add(MachineInfo.read(buffer));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(machines.size());
        for (MachineInfo machine : machines) {
            machine.write(buffer);
        }
    }

    public static class Handler implements IMessageHandler<PacketDynamic, IMessage> {

        @Override
        public IMessage onMessage(PacketDynamic message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> ClientTerminalData.updateDynamic(message.machines));
            return null;
        }
    }
}
