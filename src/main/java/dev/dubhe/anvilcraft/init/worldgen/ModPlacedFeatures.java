package dev.dubhe.anvilcraft.init.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> MEGA_CRATER = key("crater_mega");
    public static final ResourceKey<PlacedFeature> LARGE_CRATER = key("crater_large");
    public static final ResourceKey<PlacedFeature> SMALL_CRATER = key("crater_small");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        PlacementUtils.register(
            context,
            MEGA_CRATER,
            context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(ModConfiguredFeatures.MEGA_CRATER),
            RarityFilter.onAverageOnceEvery(256),
            InSquarePlacement.spread(),
            PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
            BiomeFilter.biome()
        );
        PlacementUtils.register(
            context,
            LARGE_CRATER,
            context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(ModConfiguredFeatures.LARGE_CRATER),
            RarityFilter.onAverageOnceEvery(12),
            InSquarePlacement.spread(),
            PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
            BiomeFilter.biome()
        );
        PlacementUtils.register(
            context,
            SMALL_CRATER,
            context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(ModConfiguredFeatures.SMALL_CRATER),
            RarityFilter.onAverageOnceEvery(4),
            InSquarePlacement.spread(),
            PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
            BiomeFilter.biome()
        );
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, AnvilCraft.of(name));
    }
}
