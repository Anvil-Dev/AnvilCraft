package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

    private static final String MEKANISM_MODID = "mekanism";
    // mod tags
    public static final TagKey<Block> UNDER_CAULDRON = bind("under_cauldron");
    public static final TagKey<Block> MAGNET = bind("magnet");
    public static final TagKey<Block> REDSTONE_TORCH = bind("redstone_torch");
    public static final TagKey<Block> MUSHROOM_BLOCK = bind("mushroom_block");
    public static final TagKey<Block> CANT_BROKEN_ANVIL = bind("cant_broken_anvil");
    public static final TagKey<Block> NON_MAGNETIC = bind("non_magnetic");
    public static final TagKey<Block> HAMMER_REMOVABLE = bind("hammer_removable");
    public static final TagKey<Block> HAMMER_CHANGEABLE = bind("hammer_changeable");
    public static final TagKey<Block> OVERSEER_BASE = bind("overseer_base");
    public static final TagKey<Block> BLOCK_DEVOURER_CHAIN_DEVOURING = bind("block_devourer_chain_devouring");
    public static final TagKey<Block> BLOCK_DEVOURER_PROBABILITY_DROPPING = bind("block_devourer_probability_dropping");
    public static final TagKey<Block> LASER_CAN_PASS_THROUGH = bind("laser_can_pass_though");
    public static final TagKey<Block> END_PORTAL_UNABLE_CHANGE = bind("end_portal_unable_change");
    public static final TagKey<Block> NEUTRONIUM_CANNOT_PASS_THROUGH = bind("neutronium_cannot_pass_through");
    public static final TagKey<Block> VOID_DECAY_PRODUCTS = bind("void_decay_products");
    public static final TagKey<Block> CRAFTING_MATRIX_ELEMENT = bind("crafting_matrix_element");
    public static final TagKey<Block> SPECTRAL_CAN_THROUGH = bind("spectral_can_through");
    public static final TagKey<Block> HEATABLE_BLOCKS = bind("heatable_blocks");
    public static final TagKey<Block> HEATED_BLOCKS = bind("heated_blocks");
    public static final TagKey<Block> REDHOT_BLOCKS = bind("redhot_blocks");
    public static final TagKey<Block> GLOWING_BLOCKS = bind("glowing_blocks");
    public static final TagKey<Block> INCANDESCENT_BLOCKS = bind("incandescent_blocks");
    public static final TagKey<Block> OVERHEATED_BLOCKS = bind("overheated_blocks");
    public static final TagKey<Block> SLIDING_RAILS = bind("sliding_rails");
    public static final TagKey<Block> STICKABLE_WITH_SLIDING_RAILS = bind("stickable_with_sliding_rails");
    public static final TagKey<Block> OVERHEATABLE = bind("overheatable");
    public static final TagKey<Block> ANVIL_TIER_0 = bind("anvil_tier_0");
    public static final TagKey<Block> ANVIL_TIER_1 = bind("anvil_tier_1");
    public static final TagKey<Block> ANVIL_TIER_2 = bind("anvil_tier_2");
    public static final TagKey<Block> ANVIL_TIER_3 = bind("anvil_tier_3");

    // common tags
    public static final TagKey<Block> ORES_TUNGSTEN = bindC("ores/tungsten");
    public static final TagKey<Block> ORES_TITANIUM = bindC("ores/titanium");
    public static final TagKey<Block> ORES_ZINC = bindC("ores/zinc");
    public static final TagKey<Block> ORES_TIN = bindC("ores/tin");
    public static final TagKey<Block> ORES_LEAD = bindC("ores/lead");
    public static final TagKey<Block> ORES_SILVER = bindC("ores/silver");
    public static final TagKey<Block> ORES_URANIUM = bindC("ores/uranium");
    public static final TagKey<Block> ORES_VOID_MATTER = bindC("ores/void_matter");
    public static final TagKey<Block> ORES_EARTH_CORE_SHARD = bindC("ores/earth_core_shard");

    public static final TagKey<Block> STORAGE_BLOCKS_RAW_TUNGSTEN = bindC("storage_blocks/raw_tungsten");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_TITANIUM = bindC("storage_blocks/raw_titanium");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_ZINC = bindC("storage_blocks/raw_zinc");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_TIN = bindC("storage_blocks/raw_tin");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_LEAD = bindC("storage_blocks/raw_lead");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_SILVER = bindC("storage_blocks/raw_silver");
    public static final TagKey<Block> STORAGE_BLOCKS_RAW_URANIUM = bindC("storage_blocks/raw_uranium");

    public static final TagKey<Block> STORAGE_BLOCKS_VOID_MATTER = bindC("storage_blocks/void_matter");
    public static final TagKey<Block> STORAGE_BLOCKS_EARTH_CORE_SHARD = bindC("storage_blocks/earth_core_shard");
    public static final TagKey<Block> STORAGE_BLOCKS_MULTIPHASE_MATTER = bindC("storage_blocks/multiphase_matter");

    public static final TagKey<Block> STORAGE_BLOCKS_TUNGSTEN = bindC("storage_blocks/tungsten");
    public static final TagKey<Block> STORAGE_BLOCKS_TITANIUM = bindC("storage_blocks/titanium");
    public static final TagKey<Block> STORAGE_BLOCKS_ZINC = bindC("storage_blocks/zinc");
    public static final TagKey<Block> STORAGE_BLOCKS_TIN = bindC("storage_blocks/tin");
    public static final TagKey<Block> STORAGE_BLOCKS_LEAD = bindC("storage_blocks/lead");
    public static final TagKey<Block> STORAGE_BLOCKS_SILVER = bindC("storage_blocks/silver");
    public static final TagKey<Block> STORAGE_BLOCKS_URANIUM = bindC("storage_blocks/uranium");
    public static final TagKey<Block> STORAGE_BLOCKS_PLUTONIUM = bindC("storage_blocks/plutonium");
    public static final TagKey<Block> STORAGE_BLOCKS_BRONZE = bindC("storage_blocks/bronze");
    public static final TagKey<Block> STORAGE_BLOCKS_BRASS = bindC("storage_blocks/brass");
    public static final TagKey<Block> STORAGE_BLOCKS_MAGNET = bindC("storage_blocks/magnet");
    public static final TagKey<Block> STORAGE_BLOCKS_TOPAZ = bindC("storage_blocks/topaz");
    public static final TagKey<Block> STORAGE_BLOCKS_SAPPHIRE = bindC("storage_blocks/sapphire");
    public static final TagKey<Block> STORAGE_BLOCKS_RUBY = bindC("storage_blocks/ruby");
    public static final TagKey<Block> STORAGE_BLOCKS_EXP_GEM = bindC("storage_blocks/exp_gem");
    public static final TagKey<Block> STORAGE_BLOCKS_AMBER = bindC("storage_blocks/amber");
    public static final TagKey<Block> STORAGE_BLOCKS_RESIN = bindC("storage_blocks/resin");
    public static final TagKey<Block> STORAGE_BLOCKS_TRANSCENDIUM = bindC("storage_blocks/transcendium");
    public static final TagKey<Block> STORAGE_BLOCKS_FROST_METAL = bindC("storage_blocks/frost_metal");

    public static final TagKey<Block> STORAGE_BLOCKS_SUGAR = bindC("storage_blocks/sugar");
    public static final TagKey<Block> STORAGE_BLOCKS_GUNPOWDER = bindC("storage_blocks/gunpowder");
    public static final TagKey<Block> STORAGE_BLOCKS_ROTTEN_FLESH = bindC("storage_blocks/rotten_flesh");
    public static final TagKey<Block> STORAGE_BLOCKS_FLINT = bindC("storage_blocks/flint");

    public static final TagKey<Block> INCORRECT_FOR_EMBER_TOOL = bind("incorrect_for_ember_tool");
    public static final TagKey<Block> INCORRECT_FOR_TRANSCENDIUM_TOOL = bind("incorrect_for_transcendium_tool");

    public static final TagKey<Block> NEEDS_EMBER_TOOL = bind("needs_ember_tool");
    public static final TagKey<Block> NEEDS_NETHERITE_TOOL = bind("needs_netherite_tool");
    public static final TagKey<Block> NEEDS_TRANSCENDIUM_TOOL = bind("needs_transcendium_tool");

    public static final TagKey<Block> ANVIL_HAMMER_BLACKLIST = bind("anvil_hammer_blacklist");
    public static final TagKey<Block> DEVOUR_BLACKLIST = bind("devour_blacklist");

    public static final TagKey<Block> FELLING_APPLICABLE = bind("felling_applicable");
    public static final TagKey<Block> CLEANING_APPLICABLE = bind("cleaning_applicable");

    public static final TagKey<Block> COLLISION_IMMUNE = bind("collision_immune");

    // mekanism tags
    public static final TagKey<Block> MEKANISM_CARDBOARD_BOX_BLACKLIST = bindMekanism("cardboard_blacklist");

    private static TagKey<Block> bindC(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    private static TagKey<Block> bindMekanism(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MEKANISM_MODID, id));
    }

    private static TagKey<Block> bind(String id) {
        return TagKey.create(Registries.BLOCK, AnvilCraft.of(id));
    }

    public static final Object2ObjectMap<Color, TagKey<Block>> DYED_COLORS = initDyedTags();

    public static Object2ObjectMap<Color, TagKey<Block>> initDyedTags() {
        Object2ObjectMap<Color, TagKey<Block>> map = new Object2ObjectOpenHashMap<>();
        for (Color color : Color.values()) {
            map.put(color, bindC("dyed/" + color));
        }
        return map;
    }
}
