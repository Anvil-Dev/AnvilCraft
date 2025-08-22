package dev.dubhe.anvilcraft.init.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;

public class ModBiomes {
    public static final ResourceKey<Biome> MUN = key("mun");

    public static void bootstrap(BootstrapContext<Biome> context) {
        context.register(MUN, mun(context));
    }

    private static Biome mun(BootstrapContext<Biome> context) {
        return new Biome.BiomeBuilder()
            .hasPrecipitation(false)
            .temperature(0.2f)
            .downfall(0)
            .generationSettings(
                new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER))
                    .addFeature(GenerationStep.Decoration.RAW_GENERATION, ModPlacedFeatures.MEGA_CRATER)
                    .addFeature(GenerationStep.Decoration.RAW_GENERATION, ModPlacedFeatures.LARGE_CRATER)
                    .addFeature(GenerationStep.Decoration.RAW_GENERATION, ModPlacedFeatures.SMALL_CRATER)
                    .build()
            )
            .mobSpawnSettings(new MobSpawnSettings.Builder().build())
            .specialEffects(
                new BiomeSpecialEffects.Builder()
                    .fogColor(0x000000)
                    .skyColor(0x000A14)
                    .waterColor(0x383838)
                    .waterFogColor(0x242424)
                    .grassColorOverride(0xF9E698)
                    .foliageColorOverride(0xF9E698)
                    .build()
            )
            .temperatureAdjustment(Biome.TemperatureModifier.FROZEN)
            .build();
    }

    private static ResourceKey<Biome> key(String id) {
        return ResourceKey.create(Registries.BIOME, AnvilCraft.of(id));
    }
}
