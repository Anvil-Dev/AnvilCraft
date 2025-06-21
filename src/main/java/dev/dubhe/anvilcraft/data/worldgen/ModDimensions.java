package dev.dubhe.anvilcraft.data.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public class ModDimensions {

    public static void bootstrap(BootstrapContext<LevelStem> context) {

    }

    private static ResourceKey<LevelStem> register(String id) {
        return ResourceKey.create(Registries.LEVEL_STEM, AnvilCraft.of(id));
    }
}
