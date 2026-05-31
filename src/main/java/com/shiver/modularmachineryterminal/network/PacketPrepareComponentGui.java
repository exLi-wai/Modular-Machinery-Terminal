package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.ComponentGuiGroup;
import com.shiver.modularmachineryterminal.common.MachineKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.shiver.modularmachineryterminal.client.gui.ComponentGuiPager;

public class PacketPrepareComponentGui implements IMessage {

    private MachineKey key;
    private ComponentGuiGroup group = ComponentGuiGroup.INPUT;
    private int index;
    private int total;
    private BlockPos targetPos;

    public PacketPrepareComponentGui() {
    }

    public PacketPrepareComponentGui(MachineKey key, ComponentGuiGroup group, int index, int total, BlockPos targetPos) {
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

    public static class Handler implements IMessageHandler<PacketPrepareComponentGui, IMessage> {

        @Override
        public IMessage onMessage(PacketPrepareComponentGui message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new AwaitTile(message, 0));
            return null;
        }
    }

    private static class AwaitTile implements Runnable {

        private final PacketPrepareComponentGui message;
        private final int ticks;

        private AwaitTile(PacketPrepareComponentGui message, int ticks) {
            this.message = message;
            this.ticks = ticks;
        }

        @Override
        public void run() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world == null || mc.player == null) {
                return;
            }
            TileEntity tile = mc.world.getTileEntity(message.targetPos);
            if (tile != null) {
                ComponentGuiPager.set(message.key, message.group, message.index, message.total);
                TerminalNetwork.CHANNEL.sendToServer(new PacketOpenMachineComponentGui(message.key, message.group, message.index, true));
                return;
            }
            if (ticks < 10) {
                mc.addScheduledTask(new AwaitTile(message, ticks + 1));
            }
        }
    }
}
