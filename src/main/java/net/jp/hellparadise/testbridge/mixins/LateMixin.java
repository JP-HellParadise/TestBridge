package net.jp.hellparadise.testbridge.mixins;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraftforge.fml.common.Loader;

import zone.rong.mixinbooter.ILateMixinLoader;

import com.google.common.collect.ImmutableList;

public class LateMixin implements ILateMixinLoader {

    public static final List<String> modMixins = ImmutableList
        .of("appliedenergistics2", "refinedstorage", "logisticspipes");

    @Override
    public List<String> getMixinConfigs() {
        return modMixins.stream()
            .map(mod -> "mixin.testbridge." + mod + ".json")
            .collect(Collectors.toList());
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        String[] parts = mixinConfig.split("\\.");
        return parts.length != 4 || shouldEnableModMixin(parts[2]);
    }

    public boolean shouldEnableModMixin(String mod) {
        return Loader.isModLoaded(mod);
    }
}
