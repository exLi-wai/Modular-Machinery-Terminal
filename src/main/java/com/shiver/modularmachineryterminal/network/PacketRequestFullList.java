package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.server.MachineCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestFullList implements IMessage {

    private boolean includeTeamControllers = true;

    public PacketRequestFullList() {
    }

    public PacketRequestFullList(boolean includeTeamControllers) {
        this.includeTeamControllers = includeTeamControllers;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        includeTeamControllers = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(includeTeamControllers);
    }

    public static class Handler implements IMessageHandler<PacketRequestFullList, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestFullList message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TerminalNetwork.CHANNEL.sendTo(
                    MachineCache.createFullListPacket(player, message.includeTeamControllers),
                    player
            ));
            return null;
        }
    }
}
