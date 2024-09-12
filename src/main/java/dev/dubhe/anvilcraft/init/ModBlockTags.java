package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;

public class ModBlockTags {
    public static final TagKey<Block> UNDER_CAULDRON = bind("under_cauldron");
    public static final TagKey<Block> MAGNET = bind("magnet");
    public static final TagKey<Block> REDSTONE_TORCH = bind("redstone_torch");
    public static final TagKey<Block> MUSHROOM_BLOCK = bind("mushroom_block");
    public static final TagKey<Block> CANT_BROKEN_ANVIL = bind("cant_broken_anvil");
    public static final TagKey<Block> HAMMER_REMOVABLE = bind("hammer_removable");
    public static final TagKey<Block> HAMMER_CHANGEABLE = bind("hammer_changeable");
    public static final TagKey<Block> OVERSEER_BASE = bind("overseer_base");
    public static final TagKey<Block> BLOCK_DEVOURER_PROBABILITY_DROPPING =
            bind("block_devourer_probability_dropping");
    public static final TagKey<Block> LASE_CAN_PASS_THROUGH = bind("lase_can_pass_though");
    public static final TagKey<Block> END_PORTAL_UNABLE_CHANGE = bind("end_portal_unable_change");

    public static final TagKey<Block> DEEPSLATE_METAL = bindC("deepslate_metal");

    public static final TagKey<Block> GLASS_BLOCKS = bindC("glass");
    public static final TagKey<Block> GLASS_PANES = bindC("glass_panes");
    public static final TagKey<Block> ORES = bindC("ores");
    public static final TagKey<Block> ORES_IN_GROUND_NETHERRACK = bindC("ores_in_ground/netherrack");
    public static final TagKey<Block> ORES_IN_GROUND_DEEPSLATE = bindC("ores_in_ground/deepslate");

    public static final TagKey<Block> TUNGSTEN_BLOCKS = bindC("storage_blocks/tungsten_blocks");
    public static final TagKey<Block> TITANIUM_BLOCKS = bindC("storage_blocks/titanium_blocks");
    public static final TagKey<Block> ZINC_BLOCKS = bindC("storage_blocks/zinc_blocks");
    public static final TagKey<Block> TIN_BLOCKS = bindC("storage_blocks/tin_blocks");
    public static final TagKey<Block> LEAD_BLOCKS = bindC("storage_blocks/lead_blocks");
    public static final TagKey<Block> SILVER_BLOCKS = bindC("storage_blocks/silver_blocks");
    public static final TagKey<Block> URANIUM_BLOCKS = bindC("storage_blocks/uranium_blocks");
    public static final TagKey<Block> BRONZE_BLOCKS = bindC("storage_blocks/bronze_blocks");
    public static final TagKey<Block> BRASS_BLOCKS = bindC("storage_blocks/brass_blocks");

    public static final TagKey<Block> INCORRECT_FOR_AMYTHEST_TOOL =
            bind("incorrect_for_amythest_tool");
    public static final TagKey<Block> INCORRECT_FOR_EMBER_TOOL = bind("incorrect_for_amythest_tool");

    private static @NotNull TagKey<Block> bindC(String id) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    private static @NotNull TagKey<Block> bind(String id) {
        return TagKey.create(Registries.BLOCK, AnvilCraft.of(id));
    }
}
