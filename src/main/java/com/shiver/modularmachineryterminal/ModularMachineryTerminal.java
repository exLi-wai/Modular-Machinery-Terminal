package com.shiver.modularmachineryterminal;

import com.shiver.modularmachineryterminal.common.CommonProxy;
import com.shiver.modularmachineryterminal.common.registry.ModItems;
import com.shiver.modularmachineryterminal.network.RemoteContainerTracker;
import com.shiver.modularmachineryterminal.network.TerminalNetwork;
import com.shiver.modularmachineryterminal.server.MachineCache;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = ModularMachineryTerminal.MOD_ID,
        name = ModularMachineryTerminal.MOD_NAME,
        version = ModularMachineryTerminal.VERSION,
        dependencies = "required-after:modularmachinery"
)
public class ModularMachineryTerminal {

    public static final String MOD_ID = "modularmachinery_terminal";
    public static final String MOD_NAME = "Modular Machinery Terminal";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MOD_ID)
    public static ModularMachineryTerminal INSTANCE;

    @SidedProxy(
            clientSide = "com.shiver.modularmachineryterminal.client.ClientProxy",
            serverSide = "com.shiver.modularmachineryterminal.common.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModItems.init();
        TerminalNetwork.init();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new MachineCache());
        MinecraftForge.EVENT_BUS.register(new RemoteContainerTracker());
        proxy.init();
    }
}
