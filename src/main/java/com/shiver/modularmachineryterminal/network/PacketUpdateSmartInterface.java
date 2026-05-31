package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineKey;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateSmartInterface implements IMessage {

    private MachineKey machineKey;
    private BlockPos interfacePos;
    private int dataIndex;
    private float value;

    public PacketUpdateSmartInterface() {
    }

    public PacketUpdateSmartInterface(MachineKey machineKey, BlockPos interfacePos, int dataIndex, float value) {
        this.machineKey = machineKey;
        this.interfacePos = interfacePos;
        this.dataIndex = dataIndex;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        machineKey = MachineKey.read(buffer);
        interfacePos = buffer.readBlockPos();
        dataIndex = buffer.readInt();
        value = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        machineKey.write(buffer);
        buffer.writeBlockPos(interfacePos);
        buffer.writeInt(dataIndex);
        buffer.writeFloat(value);
    }

    public static class Handler implements IMessageHandler<PacketUpdateSmartInterface, IMessage> {

        @Override
        public IMessage onMessage(PacketUpdateSmartInterface message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> update(player, message));
            return null;
        }

        private static void update(EntityPlayerMP player, PacketUpdateSmartInterface message) {
            if (message.machineKey == null || message.interfacePos == null) {
                return;
            }
            WorldServer world = player.server.getWorld(message.machineKey.dimension);
            if (world == null) {
                return;
            }
            TileEntity controllerTile = world.getTileEntity(message.machineKey.pos);
            TileEntity interfaceTile = world.getTileEntity(message.interfacePos);
            if (!(controllerTile instanceof TileMultiblockMachineController) || !(interfaceTile instanceof TileSmartInterface)) {
                player.sendMessage(new TextComponentString("Smart interface not found."));
                return;
            }
            TileSmartInterface smartInterface = (TileSmartInterface) interfaceTile;
            TileSmartInterface.SmartInterfaceProvider provider = smartInterface.provideComponent();
            SmartInterfaceData data = provider.getMachineData(message.dataIndex);
            if (data == null || !message.machineKey.pos.equals(data.getPos())) {
                player.sendMessage(new TextComponentString("Smart interface binding changed."));
                return;
            }
            data.setValue(message.value);
            TileSmartInterface.onDataUpdate(smartInterface, data);
            smartInterface.markForUpdateSync();
        }
    }
}
