package dev.dubhe.anvilcraft.init.item.tabs;

import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;

public class Ingredients extends DisplayItemsGenerator {
    @Override
    public void accept() {
        this.plain(ModItems.SPONGE_GEMMULE);
        this.plain(ModItems.AMBER);
        this.plain(ModItems.RESIN);
        this.plain(ModItems.HARDEND_RESIN);
        this.plain(ModItems.WOOD_FIBER);
        this.plain(ModItems.LIME_POWDER);
        this.plain(ModItems.CRAB_CLAW);
        this.plain(ModItems.LEVITATION_POWDER);

        this.plain(ModItems.MAGNET_INGOT);
        this.plain(ModItems.ROYAL_STEEL_INGOT);
        this.plain(ModItems.FROST_METAL_INGOT);
        this.plain(ModItems.EMBER_METAL_INGOT);
        this.plain(ModItems.TRANSCENDIUM_INGOT);
        this.plain(ModItems.CURSED_GOLD_INGOT);
        this.plain(ModItems.ENCHANTED_GOLD_INGOT);
        this.plain(ModItems.ZINC_INGOT);
        this.plain(ModItems.TIN_INGOT);
        this.plain(ModItems.TITANIUM_INGOT);
        this.plain(ModItems.TUNGSTEN_INGOT);
        this.plain(ModItems.LEAD_INGOT);
        this.plain(ModItems.SILVER_INGOT);
        this.plain(ModItems.URANIUM_INGOT);
        this.plain(ModItems.PLUTONIUM_INGOT);
        this.plain(ModItems.BRONZE_INGOT);
        this.plain(ModItems.BRASS_INGOT);

        this.plain(ModItems.ZINC_NUGGET);
        this.plain(ModItems.TIN_NUGGET);
        this.plain(ModItems.TITANIUM_NUGGET);
        this.plain(ModItems.TUNGSTEN_NUGGET);
        this.plain(ModItems.LEAD_NUGGET);
        this.plain(ModItems.SILVER_NUGGET);
        this.plain(ModItems.URANIUM_NUGGET);
        this.plain(ModItems.PLUTONIUM_NUGGET);
        this.plain(ModItems.BRONZE_NUGGET);
        this.plain(ModItems.BRASS_NUGGET);
        this.plain(ModItems.COPPER_NUGGET);
        this.plain(ModItems.ROYAL_STEEL_NUGGET);
        this.plain(ModItems.FROST_METAL_NUGGET);
        this.plain(ModItems.EMBER_METAL_NUGGET);
        this.plain(ModItems.TRANSCENDIUM_NUGGET);
        this.plain(ModItems.CURSED_GOLD_NUGGET);
        this.plain(ModItems.ENCHANTED_GOLD_NUGGET);

        this.plain(ModItems.RAW_ZINC);
        this.plain(ModItems.RAW_TIN);
        this.plain(ModItems.RAW_TITANIUM);
        this.plain(ModItems.RAW_TUNGSTEN);
        this.plain(ModItems.RAW_LEAD);
        this.plain(ModItems.RAW_SILVER);
        this.plain(ModItems.RAW_URANIUM);

        // Gems and progression materials.
        this.plain(ModItems.TOPAZ);
        this.plain(ModItems.RUBY);
        this.plain(ModItems.SAPPHIRE);
        this.plain(ModItems.EXP_GEM);
        this.plain(ModItems.CIRCUIT_BOARD);
        this.plain(ModItems.PROCESSOR);

        // Smithing templates.
        this.plain(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE);
        this.plain(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE);
        this.plain(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE);
        this.plain(ModItems.PERMUTATION_TEMPLATE_ITEM);
        this.plain(ModItems.DEFORMATION_TEMPLATE_ITEM);
        this.plain(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE);
        this.plain(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE);
        this.plain(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE);

        // Advanced components.
        this.plain(ModItems.HEAVY_HALBERD_CORE);
        this.plain(ModItems.RESONATOR_CORE);
        this.plain(ModItems.MULTIPHASE_TRANSCENDIUM);
        this.plain(ModItems.DYSON_SPHERE_COMPONENT);
        this.plain(ModItems.PENROSE_SPHERE_COMPONENT);
        this.plain(ModItems.MATTER_DECOMPRESSOR_COMPONENT);
        this.plain(ModItems.WORMHOLE_STABILIZER_COMPONENT);
        this.plain(ModItems.STELLAR_RING_COMPONENT);
        this.plain(ModItems.MAGNETAR_COIL_COMPONENT);
        this.plain(ModItems.STELLAR_EVOLUTION_ACCELERATOR_COMPONENT);

        // Void matter and neutronium.
        this.plain(ModItems.VOID_MATTER);
        this.plain(ModItems.EXCITED_STATE_VOID_MATTER);
        this.plain(ModItems.EARTH_CORE_SHARD);
        this.plain(ModItems.MULTIPHASE_MATTER);
        this.plain(ModItems.NEGATIVE_MATTER);
        this.plain(ModItems.NEGATIVE_MATTER_NUGGET);
        this.plain(ModItems.NEUTRONIUM_INGOT);
        this.plain(ModItems.STABLE_NEUTRONIUM_INGOT);
        this.plain(ModItems.CHARGED_NEUTRONIUM_INGOT);

        // Cooking ingredients are also listed in the tools tab for quick access.
        this.plain(ModFoodItems.CREAM);
        this.plain(ModFoodItems.DOUGH);
        this.plain(ModFoodItems.FLOUR);
        this.plain(ModFoodItems.COCOA_POWDER);
        this.plain(ModFoodItems.COCOA_LIQUOR);
        this.plain(ModFoodItems.COCOA_BUTTER);

        // Fluid ingredients.
        this.plain(ModItems.EXP_BUCKET);
        this.plain(ModItems.OIL_BUCKET);
        this.plain(ModItems.MELT_GEM_BUCKET);
        this.plain(ModItems.HYDROGEN_BUCKET);
        this.plain(ModItems.OXYGEN_BUCKET);
        this.plain(ModItems.HELIUM_BUCKET);
        this.plain(ModItems.DEUTERIUM_BUCKET);
        this.plain(ModItems.XENON_BUCKET);
        this.plain(ModItems.KRYPTON_BUCKET);
        ModItems.CEMENT_BUCKETS.forEach((color, bucketItem) -> this.plain(bucketItem));
    }
}
