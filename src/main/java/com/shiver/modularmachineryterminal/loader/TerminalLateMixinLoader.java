package com.shiver.modularmachineryterminal.loader;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class TerminalLateMixinLoader implements ILateMixinLoader {

    /**
     * 返回需要加载的 Mixin 配置文件列表。
     * @return 符合条件的列表
     */
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.terminal_mmce.json");
    }
}
