package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.server.MachineCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketRequestDynamic implements IMessage {

    private final List<MachineKey> keys = new ArrayList<>();
    private boolean includeTeamControllers = true;

    public PacketRequestDynamic() {
    }

    public PacketRequestDynamic(List<MachineKey> keys) {
        this.keys.addAll(keys);
    }

    public PacketRequestDynamic(List<MachineKey> keys, boolean includeTeamControllers) {
        this.keys.addAll(keys);
        this.includeTeamControllers = includeTeamControllers;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        keys.clear();
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            keys.add(MachineKey.read(buffer));
        }
        includeTeamControllers = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(keys.size());
        for (MachineKey key : keys) {
            key.write(buffer);
        }
        buffer.writeBoolean(includeTeamControllers);
    }

    public static class Handler implements IMessageHandler<PacketRequestDynamic, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestDynamic message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TerminalNetwork.CHANNEL.sendTo(
                    MachineCache.createDynamicPacket(player, message.keys, message.includeTeamControllers),
                    player
            ));
            return null;
        }
    }
}
