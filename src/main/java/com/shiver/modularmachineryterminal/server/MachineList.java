package com.shiver.modularmachineryterminal.server;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

public final class MachineList {

    private MachineList() {
    }

    public static List<TileMultiblockMachineController> loadedMachines(EntityPlayerMP player) {
        List<TileMultiblockMachineController> controllers = new ArrayList<>();
        if (player == null || player.server == null) {
            return controllers;
        }

        for (WorldServer world : player.server.worlds) {
            collectLoadedMachines(world, controllers);
        }
        return controllers;
    }

    private static void collectLoadedMachines(WorldServer world, List<TileMultiblockMachineController> controllers) {
        if (world == null) {
            return;
        }

        for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
            if (tile instanceof TileMultiblockMachineController) {
                controllers.add((TileMultiblockMachineController) tile);
            }
        }
    }
}
