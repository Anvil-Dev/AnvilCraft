package dev.dubhe.anvilcraft.data.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class ModBiomes {
    public static void bootstrap(BootstrapContext<Biome> context) {

    }

    private static ResourceKey<Biome> key(String id) {
        return ResourceKey.create(Registries.BIOME, AnvilCraft.of(id));
    }
}
