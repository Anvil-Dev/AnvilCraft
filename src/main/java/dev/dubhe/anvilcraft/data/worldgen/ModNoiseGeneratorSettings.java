package dev.dubhe.anvilcraft.data.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class ModNoiseGeneratorSettings {

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {

    }

    private static ResourceKey<NoiseGeneratorSettings> key(String id) {
        return ResourceKey.create(Registries.NOISE_SETTINGS, AnvilCraft.of(id));
    }
}
