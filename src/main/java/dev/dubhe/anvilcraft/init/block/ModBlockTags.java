package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

    private static final String MEKANISM_MODID = "mekanism";
    private static final String AE2_MODID = "ae2";
    // mod tags
    public static final TagKey<Block> UNDER_CAULDRON = ModBlockTags.bind("under_cauldron");
    public static final TagKey<Block> MAGNET = ModBlockTags.bind("magnet");
    public static final TagKey<Block> REDSTONE_TORCH = ModBlockTags.bind("redstone_torch");
    public static final TagKey<Block> MUSHROOM_BLOCK = ModBlockTags.bind("mushroom_block");
    public static final TagKey<Block> CANT_BROKEN_ANVIL = ModBlockTags.bind("cant_broken_anvil");
    public static final TagKey<Block> NON_MAGNETIC = ModBlockTags.bind("non_magnetic");
    public static final TagKey<Block> HAMMER_REMOVABLE = ModBlockTags.bind("hammer_removable");
    public static final TagKey<Block> HAMMER_CHANGEABLE = ModBlockTags.bind("hammer_changeable");
    public static final TagKey<Block> OVERSEER_BASE = ModBlockTags.bind("overseer_base");
    public static final TagKey<Block> OVERSEER_BASE_TIER_0 = ModBlockTags.bind("overseer_base_tier_0");
    public static final TagKey<Block> OVERSEER_BASE_TIER_1 = ModBlockTags.bind("overseer_base_tier_1");
    public static final TagKey<Block> OVERSEER_BASE_TIER_2 = ModBlockTags.bind("overseer_base_tier_2");
    public static final TagKey<Block> OVERSEER_BASE_TIER_3 = ModBlockTags.bind("overseer_base_tier_3");
    public static final TagKey<Block> ROYAL_SERIES = ModBlockTags.bind("royal_series");
    public static final TagKey<Block> EMBER_SERIES = ModBlockTags.bind("ember_series");
    public static final TagKey<Block> FROST_SERIES = ModBlockTags.bind("frost_series");
    public static final TagKey<Block> BLOCK_DEVOURER_CHAIN_DEVOURING = ModBlockTags.bind("block_devourer_chain_devouring");
    public static final TagKey<Block> BLOCK_DEVOURER_PROBABILITY_DROPPING = ModBlockTags.bind("block_devourer_probability_dropping");
    public static final TagKey<Block> LASER_CAN_PASS_THROUGH = ModBlockTags.bind("laser_can_pass_though");
    public static final TagKey<Block> END_PORTAL_UNABLE_CHANGE = ModBlockTags.bind("end_portal_unable_change");
    public static final TagKey<Block> NEUTRONIUM_CANNOT_PASS_THROUGH = ModBlockTags.bind("neutronium_cannot_pass_through");
    public static final TagKey<Block> VOID_DECAY_PRODUCTS = ModBlockTags.bind("void_decay_products");
    public static final TagKey<Block> CRAFTING_MATRIX_ELEMENT = ModBlockTags.bind("crafting_matrix_element");
    public static final TagKey<Block> SPECTRAL_CAN_THROUGH = ModBlockTags.bind("spectral_can_through");
    public static final TagKey<Block> HEATABLE_BLOCKS = ModBlockTags.bind("heatable_blocks");
    public static final TagKey<Block> HEATED_BLOCKS = ModBlockTags.bind("heated_blocks");
    public static final TagKey<Block> REDHOT_BLOCKS = ModBlockTags.bind("redhot_blocks");
    public static final TagKey<Block> GLOWING_BLOCKS = ModBlockTags.bind("glowing_blocks");
    public static final TagKey<Block> INCANDESCENT_BLOCKS = ModBlockTags.bind("incandescent_blocks");
    public static final TagKey<Block> OVERHEATED_BLOCKS = ModBlockTags.bind("overheated_blocks");
    public static final TagKey<Block> SLIDING_RAILS = ModBlockTags.bind("sliding_rails");
    public static final TagKey<Block> STICKABLE_WITH_SLIDING_RAILS = ModBlockTags.bind("stickable_with_sliding_rails");
    public static final TagKey<Block> OVERHEATABLE = ModBlockTags.bind("overheatable");
    public static final TagKey<Block> ANVIL_TIER_0 = ModBlockTags.bind("anvil_tier_0");
    public static final TagKey<Block> ANVIL_TIER_1 = ModBlockTags.bind("anvil_tier_1");
    public static final TagKey<Block> ANVIL_TIER_2 = ModBlockTags.bind("anvil_tier_2");
    public static final TagKey<Block> ANVIL_TIER_3 = ModBlockTags.bind("anvil_tier_3");
    public static final TagKey<Block> GIANT_ANVIL = ModBlockTags.bind("giant_anvil");
    public static final TagKey<Block> SLIDING_RAIL_STOP_LIKE = ModBlockTags.bind("sliding_rail_stop_like");

    // common tags
    public static final TagKey<Block> ORES_TUNGSTEN = ModBlockTags.bindC("ores/tungsten");
    public static final TagKey<Block> ORES_TITANIUM = ModBlockTags.bindC("ores/titanium");
    public static final TagKey<Block> ORES_ZINC = ModBlockTags.bindC("ores/zinc");
    public static final TagKey<Block> ORES_TIN = ModBlockTags.bindC("ores/tin");
    public static final TagKey<Block> ORES_LEAD = ModBlockTags.bindC("ores/lead");
    public static final TagKey<Block> ORES_SILVER = ModBlockTags.bindC("ores/silver");
    public static final TagKey<Block> ORES_URANIUM = ModBlockTags.bindC("ores/uranium");
    public static final TagKey<Block> ORES_VOID_MATTER = ModBlockTags.bindC("ores/void_matter");
    public static final TagKey<Block> ORES_EARTH_CORE_SHARD = ModBlockTags.bindC("ores/earth_core_shard");

    public static final TagKey<Block> STORAGE_BLOCKS_RAW_TUNGSTEN = ModBlockTags.bindC("storage_blocks/raw_tungsten");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_TITANIUM = ModBlockTags.bindC("storage_blocks/raw_titanium");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_ZINC = ModBlockTags.bindC("storage_blocks/raw_zinc");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_TIN = ModBlockTags.bindC("storage_blocks/raw_tin");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_LEAD = ModBlockTags.bindC("storage_blocks/raw_lead");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_SILVER = ModBlockTags.bindC("storage_blocks/raw_silver");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_URANIUM = ModBlockTags.bindC("storage_blocks/raw_uranium");

    public static final TagKey<Block> STORAGE_BLOCKS_VOID_MATTER = ModBlockTags.bindC("storage_blocks/void_matter");
    public static final TagKey<Block> STORAGE_BLOCKS_EARTH_CORE_SHARD = ModBlockTags.bindC("storage_blocks/earth_core_shard");
    public static final TagKey<Block> STORAGE_BLOCKS_MULTIPHASE_MATTER = ModBlockTags.bindC("storage_blocks/multiphase_matter");

    public static final TagKey<Block> STORAGE_BLOCKS_TUNGSTEN = ModBlockTags.bindC("storage_blocks/tungsten");
    public static final TagKey<Block> STORAGE_BLOCKS_TITANIUM = ModBlockTags.bindC("storage_blocks/titanium");
    public static final TagKey<Block> STORAGE_BLOCKS_ZINC = ModBlockTags.bindC("storage_blocks/zinc");
    public static final TagKey<Block> STORAGE_BLOCKS_TIN = ModBlockTags.bindC("storage_blocks/tin");
    public static final TagKey<Block> STORAGE_BLOCKS_LEAD = ModBlockTags.bindC("storage_blocks/lead");
    public static final TagKey<Block> STORAGE_BLOCKS_SILVER = ModBlockTags.bindC("storage_blocks/silver");
    public static final TagKey<Block> STORAGE_BLOCKS_URANIUM = ModBlockTags.bindC("storage_blocks/uranium");
    public static final TagKey<Block> STORAGE_BLOCKS_PLUTONIUM = ModBlockTags.bindC("storage_blocks/plutonium");
    public static final TagKey<Block> STORAGE_BLOCKS_BRONZE = ModBlockTags.bindC("storage_blocks/bronze");
    public static final TagKey<Block> STORAGE_BLOCKS_BRASS = ModBlockTags.bindC("storage_blocks/brass");
    public static final TagKey<Block> STORAGE_BLOCKS_MAGNET = ModBlockTags.bindC("storage_blocks/magnet");
    public static final TagKey<Block> STORAGE_BLOCKS_TOPAZ = ModBlockTags.bindC("storage_blocks/topaz");
    public static final TagKey<Block> STORAGE_BLOCKS_SAPPHIRE = ModBlockTags.bindC("storage_blocks/sapphire");
    public static final TagKey<Block> STORAGE_BLOCKS_RUBY = ModBlockTags.bindC("storage_blocks/ruby");
    public static final TagKey<Block> STORAGE_BLOCKS_EXP_GEM = ModBlockTags.bindC("storage_blocks/exp_gem");
    public static final TagKey<Block> STORAGE_BLOCKS_AMBER = ModBlockTags.bindC("storage_blocks/amber");
    public static final TagKey<Block> STORAGE_BLOCKS_RESIN = ModBlockTags.bindC("storage_blocks/resin");
    public static final TagKey<Block> STORAGE_BLOCKS_TRANSCENDIUM = ModBlockTags.bindC("storage_blocks/transcendium");
    public static final TagKey<Block> STORAGE_BLOCKS_FROST_METAL = ModBlockTags.bindC("storage_blocks/frost_metal");

    public static final TagKey<Block> STORAGE_BLOCKS_SUGAR = ModBlockTags.bindC("storage_blocks/sugar");
    public static final TagKey<Block> STORAGE_BLOCKS_GUNPOWDER = ModBlockTags.bindC("storage_blocks/gunpowder");
    public static final TagKey<Block> STORAGE_BLOCKS_ROTTEN_FLESH = ModBlockTags.bindC("storage_blocks/rotten_flesh");
    public static final TagKey<Block> STORAGE_BLOCKS_FLINT = ModBlockTags.bindC("storage_blocks/flint");

    public static final TagKey<Block> INCORRECT_FOR_EMBER_TOOL = ModBlockTags.bind("incorrect_for_ember_tool");
    public static final TagKey<Block> INCORRECT_FOR_TRANSCENDIUM_TOOL = ModBlockTags.bind("incorrect_for_transcendium_tool");

    public static final TagKey<Block> NEEDS_EMBER_TOOL = ModBlockTags.bind("needs_ember_tool");
    public static final TagKey<Block> NEEDS_NETHERITE_TOOL = ModBlockTags.bind("needs_netherite_tool");
    public static final TagKey<Block> NEEDS_TRANSCENDIUM_TOOL = ModBlockTags.bind("needs_transcendium_tool");

    public static final TagKey<Block> ANVIL_HAMMER_BLACKLIST = ModBlockTags.bind("anvil_hammer_blacklist");
    public static final TagKey<Block> DEVOUR_BLACKLIST = ModBlockTags.bind("devour_blacklist");

    public static final TagKey<Block> FELLING_APPLICABLE = ModBlockTags.bind("felling_applicable");
    public static final TagKey<Block> CLEANING_APPLICABLE = ModBlockTags.bind("cleaning_applicable");
    public static final TagKey<Block> BROKEN_CRYSTALS_CLUSTERS = ModBlockTags.bind("broken_crystals_clusters");

    public static final TagKey<Block> COLLISION_IMMUNE = ModBlockTags.bind("collision_immune");

    public static final TagKey<Block> AE2_GLASS_CABLE = ModBlockTags.bindAe2("glass_cable");
    public static final TagKey<Block> AE2_COVERED_CABLE = ModBlockTags.bindAe2("covered_cable");
    public static final TagKey<Block> AE2_SMART_CABLE = ModBlockTags.bindAe2("smart_cable");
    public static final TagKey<Block> AE2_COVERED_DENSE_CABLE = ModBlockTags.bindAe2("covered_dense_cable");
    public static final TagKey<Block> AE2_SMART_DENSE_CABLE = ModBlockTags.bindAe2("smart_dense_cable");

    // vanilla tags
    public static final TagKey<Block> LIGHTNING_RODS = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("lightning_rods"));

    // mekanism tags
    public static final TagKey<Block> MEKANISM_CARDBOARD_BOX_BLACKLIST = ModBlockTags.bindMekanism("cardboard_blacklist");

    private static TagKey<Block> bindC(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", id));
    }

    @SuppressWarnings("SameParameterValue")
    private static TagKey<Block> bindMekanism(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ModBlockTags.MEKANISM_MODID, id));
    }

    @SuppressWarnings("SameParameterValue")
    private static TagKey<Block> bindAe2(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ModBlockTags.AE2_MODID, id));
    }

    private static TagKey<Block> bind(String id) {
        return TagKey.create(Registries.BLOCK, AnvilCraft.of(id));
    }

    public static final Object2ObjectMap<Color, TagKey<Block>> DYED_COLORS = ModBlockTags.initDyedTags();

    public static Object2ObjectMap<Color, TagKey<Block>> initDyedTags() {
        Object2ObjectMap<Color, TagKey<Block>> map = new Object2ObjectOpenHashMap<>();
        for (Color color : Color.values()) {
            map.put(color, ModBlockTags.bindC("dyed/" + color));
        }
        return map;
    }
}
