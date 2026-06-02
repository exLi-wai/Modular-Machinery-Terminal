package com.shiver.modularmachineryterminal;

import com.shiver.modularmachineryterminal.common.CommonProxy;
import com.shiver.modularmachineryterminal.common.registry.ModItems;
import com.shiver.modularmachineryterminal.common.TerminalConfig;
import com.shiver.modularmachineryterminal.network.RemoteContainerTracker;
import com.shiver.modularmachineryterminal.network.TerminalNetwork;
import com.shiver.modularmachineryterminal.server.MachineCache;
import com.shiver.modularmachineryterminal.modularmachinery_terminal.Tags;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = ModularMachineryTerminal.MOD_ID,
        name = ModularMachineryTerminal.MOD_NAME,
        version = ModularMachineryTerminal.VERSION,
        dependencies = "required-after:modularmachinery;required-after:mixinbooter"
)
public class ModularMachineryTerminal {

    public static final String MOD_ID = Tags.MOD_ID;
    public static final String MOD_NAME = Tags.MOD_NAME;
    public static final String VERSION = Tags.VERSION;

    @Mod.Instance(MOD_ID)
    public static ModularMachineryTerminal INSTANCE;

    @SidedProxy(
            clientSide = "com.shiver.modularmachineryterminal.client.ClientProxy",
            serverSide = "com.shiver.modularmachineryterminal.common.CommonProxy"
    )
    public static CommonProxy proxy;

    /**
     * 执行模组预初始化阶段的注册逻辑。
     * @param event 触发该逻辑的事件对象
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TerminalConfig.load(event.getSuggestedConfigurationFile());
        ModItems.init();
        TerminalNetwork.init();
        proxy.preInit();
    }

    /**
     * 执行模组初始化阶段的注册逻辑。
     * @param event 触发该逻辑的事件对象
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new MachineCache());
        MinecraftForge.EVENT_BUS.register(new RemoteContainerTracker());
        proxy.init();
    }
}
