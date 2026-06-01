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

    public PacketComponentGuiPager() {
    }

    public PacketComponentGuiPager(MachineKey key, ComponentGuiGroup group, int index, int total, BlockPos targetPos) {
        this.key = key;
        this.group = group;
        this.index = index;
        this.total = total;
        this.targetPos = targetPos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        key = MachineKey.read(buffer);
        group = ComponentGuiGroup.byId(buffer.readInt());
        index = buffer.readInt();
        total = buffer.readInt();
        targetPos = buffer.readBlockPos();
    }

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

        @Override
        public IMessage onMessage(PacketComponentGuiPager message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> ComponentGuiPager.set(message.key, message.group, message.index, message.total, message.targetPos));
            return null;
        }
    }
}
