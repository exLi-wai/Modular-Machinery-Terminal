package com.shiver.modularmachineryterminal.server;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

public final class MachineLoadingList {

    private MachineLoadingList() {
    }

    /**
     * 返回所有已加载的模块化机械控制器列表。
     * <p>
     * 遍历服务端所有维度中已加载区块的方块实体，筛选出
     * 模块化控制器 实例并返回。
     *
     * @param player 目标玩家
     * @return 已加载的控制器列表
     */
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

    /**
     * 遍历指定维度中所有已加载区块里的方块实体，
     * 将其中的模块化机械控制器收集到列表中。
     *
     * @param world       目标维度（为 null 时直接返回）
     * @param controllers 存放结果的列表
     */
    private static void collectLoadedMachines(WorldServer world, List<TileMultiblockMachineController> controllers) {
        if (world == null) {
            return;
        }
        //cme我让你飞起来
        for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
            if (tile instanceof TileMultiblockMachineController) {
                controllers.add((TileMultiblockMachineController) tile);
            }
        }
    }
}
