package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.server.MachineCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestFullList implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<PacketRequestFullList, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestFullList message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TerminalNetwork.CHANNEL.sendTo(
                    MachineCache.createFullListPacket(player),
                    player
            ));
            return null;
        }
    }
}
