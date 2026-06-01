package com.shiver.modularmachineryterminal.loader;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

/**
 * Late mixin loader that registers mixin configs for other mod classes
 * (e.g. MMCE's {@code EventHandler}).
 * <p>
 * This class MUST NOT be in the mixin package, as the Mixin framework
 * locks mixin packages and forbids loading non-mixin classes from them.
 * <p>
 * MixinBooter discovers this class automatically by scanning for
 * {@link ILateMixinLoader} implementations.
 */
@SuppressWarnings("unused")
public class TerminalLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.terminal_mmce.json");
    }
}
