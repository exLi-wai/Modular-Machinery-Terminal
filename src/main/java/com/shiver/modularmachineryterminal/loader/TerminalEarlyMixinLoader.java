package com.shiver.modularmachineryterminal.loader;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Coremod plugin that registers early mixin configs for vanilla Minecraft
 * classes (e.g. {@code EntityPlayerMP}).
 * <p>
 * This class MUST NOT be in the mixin package, as the Mixin framework
 * locks mixin packages and forbids loading non-mixin classes from them.
 */
@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class TerminalEarlyMixinLoader implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
        Mixins.addConfiguration("mixins.terminal_minecraft.json");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
