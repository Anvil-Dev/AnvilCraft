package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;

public final class ShatteredPlanet {
    public static final int AGE_ANVIL_COUNT = 13;
    public static final int MASS_ANVIL_COUNT = 13;

    private ShatteredPlanet() {
    }

    public static SpecialCelestialBodyData createBody() {
        return new SpecialCelestialBodyData(
            AnvilCraft.of("shattered_planet").toString(),
            "shattered_planet",
            9,
            0f,
            4,
            0,
            Temperature.SCORCHED,
            false,
            LiquidCoverage.MEDIUM,
            false,
            true,
            "planet_shattered",
            null,
            null
        );
    }

    public static PlanetaryResourceSet createResources() {
        PlanetaryResourceSet resources = new PlanetaryResourceSet();
        resources.addMineral(item(AnvilCraft.of("raw_tungsten"), 30));
        resources.addMineral(item(ResourceLocation.withDefaultNamespace("raw_gold"), 30));
        resources.addMineral(item(AnvilCraft.of("raw_silver"), 30));
        resources.addMineral(item(AnvilCraft.of("earth_core_shard"), 10));
        resources.addFluid(new PlanetaryResourceSet.WeightedFluidStack(
            ResourceLocation.withDefaultNamespace("lava"),
            100
        ));
        return resources;
    }

    private static PlanetaryResourceSet.WeightedItemStack item(ResourceLocation id, int weight) {
        return new PlanetaryResourceSet.WeightedItemStack(id, weight);
    }
}
