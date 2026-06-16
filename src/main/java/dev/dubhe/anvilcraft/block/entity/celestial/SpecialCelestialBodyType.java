package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Enum defining the 6 special (hidden) celestial bodies that can be discovered
 * by consuming specific seed items in the Celestial Forging Anvil.
 *
 * <p>
 * Each body has fixed anvil requirements, preset physical properties,
 * and a set of possible seed items. For each world seed, only one seed item
 * from the set is effective — determined by a seeded random selection
 * (same pattern as RoyalPreference).
 */
@SuppressWarnings("checkstyle:LineLength")
public enum SpecialCelestialBodyType {
    OVERWORLD_LIKE(
        "overworld_like",
        32,
        14,
        20,
        16,
        "planet_overworld",
        Temperature.MILD,
        true,
        LiquidCoverage.MEDIUM,
        2,
        1.0f,
        0f,
        List.of(
            mc("grass_block"),
            mc("podzol"),
            mc("mycelium"),
            mc("dirt"),
            mc("coarse_dirt"),
            mc("rooted_dirt"),
            mc("moss_block"),
            mc("mud")
        )
    ), FLESH_PLANET(
        "flesh_planet",
        40,
        10,
        9,
        17,
        "planet_flesh",
        Temperature.HOT,
        true,
        LiquidCoverage.NONE,
        2,
        1.0f,
        22f,
        List.of(anvil("rotten_flesh_block"), mc("bone_block"))
    ), INTELLIGENT_PLANET(
        "intelligent_planet",
        58,
        12,
        12,
        18,
        "planet_intelligence",
        Temperature.HOT,
        false,
        LiquidCoverage.HIGH,
        1,
        1.0f,
        2.71828f,
        List.of(anvil("spacetime_supercomputer"))
    ), SHATTERED_PLANET(
        "shattered_planet",
        13,
        9,
        13,
        27,
        "planet_shattered",
        Temperature.SCORCHED,
        false,
        LiquidCoverage.MEDIUM,
        0,
        3.0f,
        0f,
        List.of(mc("magma_block"), mc("netherrack"), mc("blackstone"), mc("basalt"), anvil("earth_core_shard_block"))
    ), HOLLOW_PLANET(
        "hollow_planet",
        60,
        10,
        1,
        17,
        "planet_hollow",
        Temperature.HOT,
        false,
        LiquidCoverage.NONE,
        3,
        3.0f,
        45f,
        List.of(anvil("negative_matter_block"))
    ), ERROR_PLANET(
        "error_planet",
        64,
        64,
        64,
        64,
        "planet_error",
        null,
        false,
        null,
        -1,
        0f,
        0f,
        List.of(anvil("creative_generator"), mc("command_block"), mc("structure_block"))
    );

    @Getter
    private final String name;
    @Getter
    private final int time;
    @Getter
    private final int space;
    @Getter
    private final int mass;
    @Getter
    private final int energy;
    @Getter
    private final String textureName;
    @Nullable
    private final Temperature temperature;
    private final boolean hasAtmosphere;
    @Nullable
    private final LiquidCoverage liquidCoverage;
    @Getter
    private final int magneticFieldStrength;
    @Getter
    private final float rotationSpeed;
    @Getter
    private final float axialTilt;
    @Getter
    private final List<ResourceLocation> possibleSeedItems;

    SpecialCelestialBodyType(
        String name,
        int time,
        int space,
        int mass,
        int energy,
        String textureName,
        @Nullable Temperature temperature,
        boolean hasAtmosphere,
        @Nullable LiquidCoverage liquidCoverage,
        int magneticFieldStrength,
        float rotationSpeed,
        float axialTilt,
        List<ResourceLocation> possibleSeedItems
    ) {
        this.name = name;
        this.time = time;
        this.space = space;
        this.mass = mass;
        this.energy = energy;
        this.textureName = textureName;
        this.temperature = temperature;
        this.hasAtmosphere = hasAtmosphere;
        this.liquidCoverage = liquidCoverage;
        this.magneticFieldStrength = magneticFieldStrength;
        this.rotationSpeed = rotationSpeed;
        this.axialTilt = axialTilt;
        this.possibleSeedItems = possibleSeedItems;
    }

    @Nullable
    public Temperature getTemperature() {
        return temperature;
    }

    public boolean hasAtmosphere() {
        return hasAtmosphere;
    }

    @Nullable
    public LiquidCoverage getLiquidCoverage() {
        return liquidCoverage;
    }

    /**
     * Get the standalone model resource location for this special body.
     */
    public ModelResourceLocation getModelLocation() {
        return ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/" + textureName));
    }

    /**
     * Whether this is the Error Planet (special "???" display values).
     */
    public boolean isErrorPlanet() {
        return this == ERROR_PLANET;
    }

    /**
     * Whether this body uses model loading instead of manual cube rendering.
     * {@link #SHATTERED_PLANET}, {@link #HOLLOW_PLANET}, and {@link #ERROR_PLANET}
     * have non-cube geometry.
     * {@link #FLESH_PLANET} and {@link #INTELLIGENT_PLANET} use animated textures
     * which require block atlas / model loading.
     */
    public boolean needsCustomModel() {
        return this == SHATTERED_PLANET
            || this == HOLLOW_PLANET
            || this == ERROR_PLANET
            || this == FLESH_PLANET
            || this == INTELLIGENT_PLANET;
    }

    /**
     * Get the effective seed item for a given world seed.
     */
    public Item getEffectiveSeedItem(long worldSeed) {
        if (possibleSeedItems.size() <= 1) {
            return resolveItem(possibleSeedItems.getFirst());
        }
        Random random = new Random(worldSeed * 31L + this.ordinal() * 7919L);
        return resolveItem(possibleSeedItems.get(random.nextInt(possibleSeedItems.size())));
    }

    /**
     * Check if the given item is THE effective seed item for this body type
     * given the world seed.
     */
    public boolean isEffectiveSeedItem(Item consumedItem, long worldSeed) {
        Item effective = getEffectiveSeedItem(worldSeed);
        return consumedItem == effective;
    }

    /**
     * Generate the planetary resources for this special body.
     */
    public PlanetaryResourceSet generateResources() {
        return switch (this) {
            case OVERWORLD_LIKE -> genOverworldLike();
            case FLESH_PLANET -> genFleshPlanet();
            case INTELLIGENT_PLANET -> genIntelligentPlanet();
            case SHATTERED_PLANET -> genShatteredPlanet();
            case HOLLOW_PLANET -> genHollowPlanet();
            case ERROR_PLANET -> new PlanetaryResourceSet();
        };
    }

    // === Resource helpers ===

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation anvil(String path) {
        return ResourceLocation.fromNamespaceAndPath("anvilcraft", path);
    }

    private static PlanetaryResourceSet.WeightedItemStack item(ResourceLocation id, int weight) {
        return new PlanetaryResourceSet.WeightedItemStack(id, weight);
    }

    private static PlanetaryResourceSet.WeightedFluidStack fluid(ResourceLocation id, int weight) {
        return new PlanetaryResourceSet.WeightedFluidStack(id, weight);
    }

    private static Item resolveItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    // === Resource generation per type ===

    private static PlanetaryResourceSet genOverworldLike() {
        PlanetaryResourceSet r = new PlanetaryResourceSet();
        r.addMineral(item(mc("raw_copper"), 50));
        r.addMineral(item(mc("raw_iron"), 30));
        r.addMineral(item(mc("raw_gold"), 20));
        r.addFluid(fluid(mc("water"), 100));
        r.addBiologicalItem(item(mc("porkchop"), 25));
        r.addBiologicalItem(item(mc("beef"), 25));
        r.addBiologicalItem(item(mc("mutton"), 25));
        r.addBiologicalItem(item(mc("chicken"), 25));
        return r;
    }

    private static PlanetaryResourceSet genFleshPlanet() {
        PlanetaryResourceSet r = new PlanetaryResourceSet();
        r.addMineral(item(mc("rotten_flesh"), 30));
        r.addMineral(item(mc("bone"), 30));
        r.addMineral(item(mc("string"), 20));
        r.addMineral(item(mc("spider_eye"), 15));
        r.addMineral(item(mc("phantom_membrane"), 3));
        r.addMineral(item(mc("ghast_tear"), 2));
        return r;
    }

    private static PlanetaryResourceSet genIntelligentPlanet() {
        PlanetaryResourceSet r = new PlanetaryResourceSet();
        r.addFluid(fluid(anvil("exp_fluid"), 100));
        return r;
    }

    private static PlanetaryResourceSet genShatteredPlanet() {
        PlanetaryResourceSet r = new PlanetaryResourceSet();
        r.addMineral(item(anvil("raw_tungsten"), 30));
        r.addMineral(item(mc("raw_gold"), 30));
        r.addMineral(item(anvil("raw_silver"), 30));
        r.addMineral(item(anvil("earth_core_shard"), 10));
        r.addFluid(fluid(mc("lava"), 100));
        return r;
    }

    private static PlanetaryResourceSet genHollowPlanet() {
        PlanetaryResourceSet r = new PlanetaryResourceSet();
        r.addMineral(item(mc("obsidian"), 90));
        r.addMineral(item(anvil("void_matter"), 8));
        r.addMineral(item(anvil("negative_matter_nugget"), 2));
        return r;
    }
}
