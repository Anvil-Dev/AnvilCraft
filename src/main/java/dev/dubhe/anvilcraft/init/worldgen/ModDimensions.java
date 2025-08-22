package dev.dubhe.anvilcraft.init.worldgen;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.util.List;

public class ModDimensions {
    public static final ResourceKey<LevelStem> MUN = register("mun");

    public static void bootstrap(BootstrapContext<LevelStem> context) {
        context.register(MUN, mun(context));
    }

    private static LevelStem mun(BootstrapContext<LevelStem> context) {
        return new LevelStem(
            context.lookup(Registries.DIMENSION_TYPE).getOrThrow(ModDimensionTypes.MUN),
            new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(List.of(Pair.of(
                    Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                    context.lookup(Registries.BIOME).getOrThrow(ModBiomes.MUN)
                )))),
                context.lookup(Registries.NOISE_SETTINGS).getOrThrow(ModNoiseGeneratorSettings.MUN)
            )
        );
    }

    private static ResourceKey<LevelStem> register(String id) {
        return ResourceKey.create(Registries.LEVEL_STEM, AnvilCraft.of(id));
    }
}
