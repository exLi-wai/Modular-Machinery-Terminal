package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.common.MachineKey;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTeleportToMachine implements IMessage {

    private MachineKey key;

    public PacketTeleportToMachine() {
    }

    public PacketTeleportToMachine(MachineKey key) {
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

    public static class Handler implements IMessageHandler<PacketTeleportToMachine, IMessage> {

        @Override
        public IMessage onMessage(PacketTeleportToMachine message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> teleport(player, message.key));
            return null;
        }

        private static void teleport(EntityPlayerMP player, MachineKey key) {
            MinecraftServer server = player.getServer();
            if (server == null || key == null) {
                return;
            }
            WorldServer world = server.getWorld(key.dimension);
            if (world == null) {
                player.sendMessage(new TextComponentString("Dimension not found: " + key.dimension));
                return;
            }
            TileEntity tile = world.getTileEntity(key.pos);
            if (!(tile instanceof TileMultiblockMachineController)) {
                player.sendMessage(new TextComponentString("Machine controller not found."));
                return;
            }
            TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
            EnumFacing facing = controller.getControllerRotation();
            if (facing == null || facing.getAxis().isVertical()) {
                facing = EnumFacing.SOUTH;
            }
            BlockPos target = key.pos.offset(facing);
            double x = target.getX() + 0.5D;
            int y = target.getY();
            double z = target.getZ() + 0.5D;
            String command;
            if (player.dimension == key.dimension) {
                command = "tp " + player.getName() + " " + x + " " + y + " " + z;
            } else {
                command = "forge setdimension " + player.getName() + " " + key.dimension + " " + x + " " + y + " " + z;
            }
            server.getCommandManager().executeCommand(server, command);
        }
    }
}
