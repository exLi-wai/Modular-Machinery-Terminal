package com.shiver.modularmachineryterminal.loader;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class TerminalEarlyMixinLoader implements IFMLLoadingPlugin {

    /**
     * 返回核心模组需要注册的 ASM 转换器类名。
     * @return 方法执行结果
     */
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    /**
     * 返回核心模组容器类名。
     * @return 对应的文本
     */
    @Override
    public String getModContainerClass() {
        return null;
    }

    /**
     * 返回核心模组启动设置类名。
     * @return 对应的文本
     */
    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    /**
     * 接收 Forge 注入的核心模组启动数据。
     * @param data Forge 注入的数据
     */
    @Override
    public void injectData(final Map<String, Object> data) {
        Mixins.addConfiguration("mixins.terminal_minecraft.json");
    }

    /**
     * 返回访问转换器类名。
     * @return 对应的文本
     */
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
