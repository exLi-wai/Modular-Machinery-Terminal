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

    public PacketCancelComponentGuiPrepare() {
    }

    public PacketCancelComponentGuiPrepare(MachineKey key) {
        this.key = key;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        key = MachineKey.read(new PacketBuffer(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        key.write(new PacketBuffer(buf));
    }

    public static class Handler implements IMessageHandler<PacketCancelComponentGuiPrepare, IMessage> {

        @Override
        public IMessage onMessage(PacketCancelComponentGuiPrepare message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> RemoteContainerTracker.cancelPending(player, message.key));
            return null;
        }
    }
}
