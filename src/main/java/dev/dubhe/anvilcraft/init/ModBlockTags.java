package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;

public class ModBlockTags {
    // mod tags
    public static final TagKey<Block> UNDER_CAULDRON = bind("under_cauldron");
    public static final TagKey<Block> MAGNET = bind("magnet");
    public static final TagKey<Block> REDSTONE_TORCH = bind("redstone_torch");
    public static final TagKey<Block> MUSHROOM_BLOCK = bind("mushroom_block");
    public static final TagKey<Block> CANT_BROKEN_ANVIL = bind("cant_broken_anvil");
    public static final TagKey<Block> HAMMER_REMOVABLE = bind("hammer_removable");
    public static final TagKey<Block> HAMMER_CHANGEABLE = bind("hammer_changeable");
    public static final TagKey<Block> OVERSEER_BASE = bind("overseer_base");
    public static final TagKey<Block> BLOCK_DEVOURER_PROBABILITY_DROPPING = bind("block_devourer_probability_dropping");
    public static final TagKey<Block> LASE_CAN_PASS_THROUGH = bind("lase_can_pass_though");
    public static final TagKey<Block> END_PORTAL_UNABLE_CHANGE = bind("end_portal_unable_change");

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

    public static final TagKey<Block> STORAGE_BLOCKS_TUNGSTEN = bindC("storage_blocks/tungsten");
    public static final TagKey<Block> STORAGE_BLOCKS_TITANIUM = bindC("storage_blocks/titanium");
    public static final TagKey<Block> STORAGE_BLOCKS_ZINC = bindC("storage_blocks/zinc");
    public static final TagKey<Block> STORAGE_BLOCKS_TIN = bindC("storage_blocks/tin");
    public static final TagKey<Block> STORAGE_BLOCKS_LEAD = bindC("storage_blocks/lead");
    public static final TagKey<Block> STORAGE_BLOCKS_SILVER = bindC("storage_blocks/silver");
    public static final TagKey<Block> STORAGE_BLOCKS_URANIUM = bindC("storage_blocks/uranium");
    public static final TagKey<Block> STORAGE_BLOCKS_BRONZE = bindC("storage_blocks/bronze");
    public static final TagKey<Block> STORAGE_BLOCKS_BRASS = bindC("storage_blocks/brass");

    public static final TagKey<Block> INCORRECT_FOR_AMYTHEST_TOOL = bind("incorrect_for_amythest_tool");
    public static final TagKey<Block> INCORRECT_FOR_EMBER_TOOL = bind("incorrect_for_ember_tool");

    private static @NotNull TagKey<Block> bindC(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    private static @NotNull TagKey<Block> bind(String id) {
        return TagKey.create(Registries.BLOCK, AnvilCraft.of(id));
    }
}
