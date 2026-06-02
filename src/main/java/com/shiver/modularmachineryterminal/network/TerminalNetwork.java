package com.shiver.modularmachineryterminal.network;

import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class TerminalNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(ModularMachineryTerminal.MOD_ID);

    /**
     * 创建 TerminalNetwork 实例。
     */
    private TerminalNetwork() {
    }

    /**
     * 执行模组初始化阶段的注册逻辑。
     */
    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(PacketRequestFullList.Handler.class, PacketRequestFullList.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketFullList.Handler.class, PacketFullList.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketRequestDynamic.Handler.class, PacketRequestDynamic.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketDynamic.Handler.class, PacketDynamic.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketTeleportToMachine.Handler.class, PacketTeleportToMachine.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketOpenMachineComponentGui.Handler.class, PacketOpenMachineComponentGui.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketPrepareComponentGui.Handler.class, PacketPrepareComponentGui.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketComponentGuiPager.Handler.class, PacketComponentGuiPager.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketCancelComponentGuiPrepare.Handler.class, PacketCancelComponentGuiPrepare.class, id, Side.SERVER);
    }
}
