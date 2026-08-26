package dev.dubhe.anvilcraft.init.item.tabs;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;

public class BuildingBlocks extends DisplayItemsGenerator {
    @Override
    public void accept() {
        // Royal steel construction set.
        this.plain(ModBlocks.ROYAL_STEEL_BLOCK);
        this.plain(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK);
        this.plain(ModBlocks.CUT_ROYAL_STEEL_BLOCK);
        this.plain(ModBlocks.CUT_ROYAL_STEEL_PILLAR);
        this.plain(ModBlocks.CUT_ROYAL_STEEL_SLAB);
        this.plain(ModBlocks.CUT_ROYAL_STEEL_STAIRS);

        // Frost metal construction set.
        this.plain(ModBlocks.FROST_METAL_BLOCK);
        this.plain(ModBlocks.CUT_FROST_METAL_BLOCK);
        this.plain(ModBlocks.CUT_FROST_METAL_PILLAR);
        this.plain(ModBlocks.CUT_FROST_METAL_SLAB);
        this.plain(ModBlocks.CUT_FROST_METAL_STAIRS);
        this.plain(ModBlocks.FROST_DECO_BLOCK);
        this.plain(ModBlocks.FROST_DECO_OUTLINE);

        // Ember metal construction set.
        this.plain(ModBlocks.EMBER_METAL_BLOCK);
        this.plain(ModBlocks.CUT_EMBER_METAL_BLOCK);
        this.plain(ModBlocks.CUT_EMBER_METAL_PILLAR);
        this.plain(ModBlocks.CUT_EMBER_METAL_SLAB);
        this.plain(ModBlocks.CUT_EMBER_METAL_STAIRS);
        this.plain(ModBlocks.EMBER_DECO_BLOCK);
        this.plain(ModBlocks.EMBER_DECO_OUTLINE);

        // Transcendium construction set.
        this.plain(ModBlocks.TRANSCENDIUM_BLOCK);
        this.plain(ModBlocks.TRANSCENDENCE_DECO_BLOCK);
        this.plain(ModBlocks.TRANSCENDENCE_DECO_OUTLINE);

        // Heavy iron construction set.
        this.plain(ModBlocks.HEAVY_IRON_BLOCK);
        this.plain(ModBlocks.POLISHED_HEAVY_IRON_BLOCK);
        this.plain(ModBlocks.POLISHED_HEAVY_IRON_SLAB);
        this.plain(ModBlocks.POLISHED_HEAVY_IRON_STAIRS);
        this.plain(ModBlocks.CUT_HEAVY_IRON_BLOCK);
        this.plain(ModBlocks.CUT_HEAVY_IRON_SLAB);
        this.plain(ModBlocks.CUT_HEAVY_IRON_STAIRS);
        this.plain(ModBlocks.HEAVY_IRON_PLATE);
        this.plain(ModBlocks.HEAVY_IRON_COLUMN);
        this.plain(ModBlocks.HEAVY_IRON_BEAM);
        this.plain(ModBlocks.HEAVY_IRON_WALL);
        this.plain(ModBlocks.HEAVY_IRON_DOOR);
        this.plain(ModBlocks.HEAVY_IRON_TRAPDOOR);

        // Bronze and brass construction sets.
        this.plain(ModBlocks.BRONZE_BLOCK);
        this.plain(ModBlocks.CUT_BRONZE_BLOCK);
        this.plain(ModBlocks.CUT_BRONZE_STAIRS);
        this.plain(ModBlocks.CUT_BRONZE_SLAB);
        this.plain(ModBlocks.CUT_BRONZE_PILLAR);
        this.plain(ModBlocks.CHISELED_BRONZE_BLOCK);
        this.plain(ModBlocks.BRASS_BLOCK);
        this.plain(ModBlocks.CUT_BRASS_BLOCK);
        this.plain(ModBlocks.CUT_BRASS_STAIRS);
        this.plain(ModBlocks.CUT_BRASS_SLAB);
        this.plain(ModBlocks.CUT_BRASS_PILLAR);
        this.plain(ModBlocks.CHISELED_BRASS_BLOCK);

        // Magnet blocks are cross-listed here because they are both machinery and building blocks.
        this.plain(ModBlocks.MAGNET_BLOCK);
        this.plain(ModBlocks.HOLLOW_MAGNET_BLOCK);
        this.plain(ModBlocks.FERRITE_CORE_MAGNET_BLOCK);

        // Standard metal blocks.
        this.plain(ModBlocks.CURSED_GOLD_BLOCK);
        this.plain(ModBlocks.ENCHANTED_GOLD_BLOCK);
        this.plain(ModBlocks.ZINC_BLOCK);
        this.plain(ModBlocks.TIN_BLOCK);
        this.plain(ModBlocks.TITANIUM_BLOCK);
        this.plain(ModBlocks.TUNGSTEN_BLOCK);
        this.plain(ModBlocks.LEAD_BLOCK);
        this.plain(ModBlocks.SILVER_BLOCK);
        this.plain(ModBlocks.URANIUM_BLOCK);
        this.plain(ModBlocks.PLUTONIUM_BLOCK);

        // Gemstone, resin and glass blocks.
        this.plain(ModBlocks.TOPAZ_BLOCK);
        this.plain(ModBlocks.RUBY_BLOCK);
        this.plain(ModBlocks.SAPPHIRE_BLOCK);
        this.plain(ModBlocks.CHROMATIC_STONE);
        this.plain(ModBlocks.EXP_GEM_BLOCK);
        this.plain(ModBlocks.RESIN_BLOCK);
        this.plain(ModBlocks.AMBER_BLOCK);
        this.plain(ModBlocks.MOB_AMBER_BLOCK);
        this.plain(ModBlocks.RESENTFUL_AMBER_BLOCK);
        this.plain(ModBlocks.TEMPERING_GLASS);
        this.plain(ModBlocks.FROST_GLASS);
        this.plain(ModBlocks.EMBER_GLASS);

        // Reinforced concrete is grouped by color, keeping all four shapes together.
        for (Color color : Color.values()) {
            this.plain(ModBlocks.REINFORCED_CONCRETES.get(color));
            this.plain(ModBlocks.REINFORCED_CONCRETE_SLABS.get(color));
            this.plain(ModBlocks.REINFORCED_CONCRETE_STAIRS.get(color));
            this.plain(ModBlocks.REINFORCED_CONCRETE_WALLS.get(color));
        }

        // Natural terrain, raw metals and ores.
        this.plain(ModBlocks.CINERITE);
        this.plain(ModBlocks.QUARTZ_SAND);
        this.plain(ModBlocks.LEVITATION_POWDER_BLOCK);
        this.plain(ModBlocks.CONTROLLABLE_SAND);
        this.plain(ModBlocks.NETHER_DUST);
        this.plain(ModBlocks.END_DUST);
        this.plain(ModBlocks.RAW_ZINC_BLOCK);
        this.plain(ModBlocks.RAW_TIN_BLOCK);
        this.plain(ModBlocks.RAW_TITANIUM_BLOCK);
        this.plain(ModBlocks.RAW_TUNGSTEN_BLOCK);
        this.plain(ModBlocks.RAW_LEAD_BLOCK);
        this.plain(ModBlocks.RAW_SILVER_BLOCK);
        this.plain(ModBlocks.RAW_URANIUM_BLOCK);
        this.plain(ModBlocks.DEEPSLATE_ZINC_ORE);
        this.plain(ModBlocks.DEEPSLATE_TIN_ORE);
        this.plain(ModBlocks.DEEPSLATE_TITANIUM_ORE);
        this.plain(ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
        this.plain(ModBlocks.DEEPSLATE_LEAD_ORE);
        this.plain(ModBlocks.DEEPSLATE_SILVER_ORE);
        this.plain(ModBlocks.DEEPSLATE_URANIUM_ORE);
        this.plain(ModBlocks.VOID_STONE);
        this.plain(ModBlocks.EARTH_CORE_SHARD_ORE);
        this.plain(ModBlocks.STURDY_DEEPSLATE);
        this.plain(ModBlocks.ANCIENT_SEA_REEF);

        // Advanced matter and confinement structures.
        this.plain(ModBlocks.VOID_MATTER_BLOCK);
        this.plain(ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK);
        this.plain(ModBlocks.EARTH_CORE_SHARD_BLOCK);
        this.plain(ModBlocks.MULTIPHASE_MATTER_BLOCK);
        this.plain(ModBlocks.NEGATIVE_MATTER_BLOCK);

        this.plain(ModBlocks.SINGULARITY_CRYSTAL);
        this.plain(ModBlocks.HYPERCUBE);

        // Heated and incandescent metal variants.
        this.plain(ModBlocks.HEATED_NETHERITE_BLOCK);
        this.plain(ModBlocks.HEATED_TUNGSTEN_BLOCK);
        this.plain(ModBlocks.REDHOT_NETHERITE_BLOCK);
        this.plain(ModBlocks.REDHOT_TUNGSTEN_BLOCK);
        this.plain(ModBlocks.GLOWING_NETHERITE_BLOCK);
        this.plain(ModBlocks.GLOWING_TUNGSTEN_BLOCK);
        this.plain(ModBlocks.INCANDESCENT_NETHERITE_BLOCK);
        this.plain(ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK);
        this.plain(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK);

        // Decorative signs and processed building materials.
        this.plain(ModBlocks.ARROW);
        this.plain(ModBlocks.CHECK_MARK);
        this.plain(ModBlocks.CROSS_MARK);
        this.plain(ModBlocks.EXCLAMATION_MARK);
        this.plain(ModBlocks.QUESTION_MARK);
        this.plain(ModBlocks.FLINT_BLOCK);
        this.plain(ModBlocks.POLISHED_FLINT_BLOCK);
        this.plain(ModBlocks.CUT_FLINT_BLOCK);
        this.plain(ModBlocks.CUT_FLINT_SLAB_BLOCK);
        this.plain(ModBlocks.CUT_FLINT_STAIRS_BLOCK);
        this.plain(ModBlocks.CUT_FLINT_PILLAR_BLOCK);
        this.plain(ModBlocks.PLYWOOD_BLOCK);
        this.plain(ModBlocks.PLYWOOD_STAIRS);
        this.plain(ModBlocks.PLYWOOD_SLAB);

        // Food and confectionery blocks.
        this.plain(ModBlocks.CAKE_BASE_BLOCK);
        this.plain(ModBlocks.CREAM_BLOCK);
        this.plain(ModBlocks.BERRY_CREAM_BLOCK);
        this.plain(ModBlocks.CHOCOLATE_CREAM_BLOCK);
        this.plain(ModBlocks.HONEY_CREAM_BLOCK);
        this.plain(ModBlocks.MATCHA_CREAM_BLOCK);
        this.plain(ModBlocks.CAKE_BLOCK);
        this.plain(ModBlocks.BERRY_CAKE_BLOCK);
        this.plain(ModBlocks.CHOCOLATE_CAKE_BLOCK);
        this.plain(ModBlocks.HONEY_CAKE_BLOCK);
        this.plain(ModBlocks.MATCHA_CAKE_BLOCK);
        this.plain(ModBlocks.LARGE_CAKE);
        this.plain(ModBlocks.CHOCOLATE_BLOCK);
        this.plain(ModBlocks.BLACK_CHOCOLATE_BLOCK);
        this.plain(ModBlocks.WHITE_CHOCOLATE_BLOCK);
        this.plain(ModBlocks.BLACK_WHITE_CHOCOLATE_BLOCK);
        this.plain(ModBlocks.CHOCOLATE_SLAB);
        this.plain(ModBlocks.BLACK_CHOCOLATE_SLAB);
        this.plain(ModBlocks.WHITE_CHOCOLATE_SLAB);
        this.plain(ModBlocks.CHOCOLATE_STAIRS);
        this.plain(ModBlocks.BLACK_CHOCOLATE_STAIRS);
        this.plain(ModBlocks.WHITE_CHOCOLATE_STAIRS);
        this.plain(ModBlocks.COOKIE_BLOCK);
        this.plain(ModBlocks.COOKIE_PILLAR);
        this.plain(ModBlocks.SUGAR_BLOCK);
        this.plain(ModBlocks.GUNPOWER_BLOCK);
        this.plain(ModBlocks.ROTTEN_FLESH_BLOCK);
    }
}
