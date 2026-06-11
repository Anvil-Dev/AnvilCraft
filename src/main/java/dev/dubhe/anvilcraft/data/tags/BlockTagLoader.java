package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class BlockTagLoader {

    private static Identifier findId(Block item) {
        return BuiltInRegistries.BLOCK.getKey(item);
    }

    /// 初始化方块标签
    ///
    /// @param provider 提供器
    public static void init(RegistrumTagsProvider<Block> provider) {
        provider.rawBuilder(ModBlockTags.REDSTONE_TORCH)
            .addElement(findId(Blocks.REDSTONE_WALL_TORCH))
            .addElement(findId(Blocks.REDSTONE_TORCH));

        provider.rawBuilder(ModBlockTags.MUSHROOM_BLOCK)
            .addElement(findId(Blocks.BROWN_MUSHROOM_BLOCK))
            .addElement(findId(Blocks.RED_MUSHROOM_BLOCK))
            .addElement(findId(Blocks.MUSHROOM_STEM));

        provider.rawBuilder(ModBlockTags.HAMMER_CHANGEABLE)
            .addElement(findId(Blocks.OBSERVER))
            .addElement(findId(Blocks.HOPPER))
            .addElement(findId(Blocks.DROPPER))
            .addElement(findId(Blocks.DISPENSER))
            .addElement(findId(Blocks.CRAFTER))
            .addElement(findId(Blocks.LIGHTNING_ROD));

        provider.rawBuilder(ModBlockTags.HAMMER_REMOVABLE)
            .addTag(BlockTags.TRAPDOORS.location())
            .addTag(BlockTags.DOORS.location())
            .addTag(BlockTags.BUTTONS.location())
            .addTag(BlockTags.PRESSURE_PLATES.location())
            .addElement(findId(Blocks.BELL))
            .addElement(findId(Blocks.REDSTONE_LAMP))
            .addElement(findId(Blocks.RAIL))
            .addElement(findId(Blocks.ACTIVATOR_RAIL))
            .addElement(findId(Blocks.DETECTOR_RAIL))
            .addElement(findId(Blocks.POWERED_RAIL))
            .addElement(findId(Blocks.NOTE_BLOCK))
            .addElement(findId(Blocks.OBSERVER))
            .addElement(findId(Blocks.HOPPER))
            .addElement(findId(Blocks.DROPPER))
            .addElement(findId(Blocks.DISPENSER))
            .addElement(findId(Blocks.CRAFTER))
            .addElement(findId(Blocks.HONEY_BLOCK))
            .addElement(findId(Blocks.SLIME_BLOCK))
            .addElement(findId(Blocks.PISTON))
            .addElement(findId(Blocks.STICKY_PISTON))
            .addElement(findId(Blocks.PISTON_HEAD))
            .addElement(findId(Blocks.LIGHTNING_ROD))
            .addElement(findId(Blocks.DAYLIGHT_DETECTOR))
            .addElement(findId(Blocks.LECTERN))
            .addElement(findId(Blocks.TRIPWIRE_HOOK))
            .addElement(findId(Blocks.SCULK_SHRIEKER))
            .addElement(findId(Blocks.LEVER))
            .addElement(findId(Blocks.SCULK_SENSOR))
            .addElement(findId(Blocks.CALIBRATED_SCULK_SENSOR))
            .addElement(findId(Blocks.REDSTONE_WIRE))
            .addElement(findId(Blocks.REDSTONE_TORCH))
            .addElement(findId(Blocks.REDSTONE_WALL_TORCH))
            .addElement(findId(Blocks.REDSTONE_BLOCK))
            .addElement(findId(Blocks.REPEATER))
            .addElement(findId(Blocks.COMPARATOR))
            .addElement(findId(Blocks.TARGET))
            .addElement(findId(Blocks.COPPER_BULB))
            .addElement(findId(Blocks.EXPOSED_COPPER_BULB))
            .addElement(findId(Blocks.WEATHERED_COPPER_BULB))
            .addElement(findId(Blocks.OXIDIZED_COPPER_BULB))
            .addElement(findId(Blocks.WAXED_COPPER_BULB))
            .addElement(findId(Blocks.WAXED_EXPOSED_COPPER_BULB))
            .addElement(findId(Blocks.WAXED_WEATHERED_COPPER_BULB))
            .addElement(findId(Blocks.WAXED_OXIDIZED_COPPER_BULB))
            .addElement(findId(Blocks.CAULDRON))
            .addElement(findId(Blocks.LAVA_CAULDRON))
            .addElement(findId(Blocks.WATER_CAULDRON))
            .addElement(findId(Blocks.POWDER_SNOW_CAULDRON))
            .addElement(findId(Blocks.CAMPFIRE))
            .addElement(findId(Blocks.STONECUTTER))
            .addElement(findId(Blocks.SCAFFOLDING))
            .addElement(findId(Blocks.ANVIL))
            .addElement(findId(Blocks.CHIPPED_ANVIL))
            .addElement(findId(Blocks.DAMAGED_ANVIL))
            .addElement(ModBlocks.HEAVY_IRON_BLOCK.getId())
            .addElement(ModBlocks.HEAVY_IRON_BEAM.getId())
            .addElement(ModBlocks.HEAVY_IRON_COLUMN.getId())
            .addElement(ModBlocks.HEAVY_IRON_PLATE.getId())
            .addElement(ModBlocks.CUT_HEAVY_IRON_BLOCK.getId())
            .addElement(ModBlocks.CUT_HEAVY_IRON_SLAB.getId())
            .addElement(ModBlocks.CUT_HEAVY_IRON_STAIRS.getId())
            .addElement(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.getId())
            .addElement(ModBlocks.POLISHED_HEAVY_IRON_SLAB.getId())
            .addElement(ModBlocks.POLISHED_HEAVY_IRON_STAIRS.getId())
            .addTag(BlockTags.SHULKER_BOXES.location());

        provider.rawBuilder(ModBlockTags.UNDER_CAULDRON)
            .addTag(BlockTags.CAMPFIRES.location())
            .addElement(findId(Blocks.MAGMA_BLOCK))
            .addElement(ModBlocks.HEATER.getId())
            .addElement(ModBlocks.CORRUPTED_BEACON.getId());

        provider.rawBuilder(ModBlockTags.BLOCK_DEVOURER_CHAIN_DEVOURING)
            .addTag(Tags.Blocks.SANDS.location())
            .addTag(Tags.Blocks.GRAVELS.location());

        provider.rawBuilder(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
            .addElement(findId(Blocks.STONE))
            .addElement(findId(Blocks.DEEPSLATE))
            .addElement(findId(Blocks.ANDESITE))
            .addElement(findId(Blocks.DIORITE))
            .addElement(findId(Blocks.GRANITE))
            .addElement(findId(Blocks.TUFF))
            .addElement(findId(Blocks.NETHERRACK))
            .addElement(findId(Blocks.BASALT))
            .addElement(findId(Blocks.BLACKSTONE))
            .addElement(findId(Blocks.END_STONE));

        provider.rawBuilder(ModBlockTags.LASER_CAN_PASS_THROUGH)
            .addTag(Tags.Blocks.GLASS_BLOCKS.location())
            .addTag(Tags.Blocks.GLASS_PANES.location())
            .addTag(BlockTags.REPLACEABLE.location());

        provider.rawBuilder(ModBlockTags.END_PORTAL_UNABLE_CHANGE)
            .addElement(findId(Blocks.DRAGON_EGG));

        provider.rawBuilder(ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)
            .addElement(findId(Blocks.END_STONE))
            .addElement(findId(Blocks.BEDROCK))
            .addElement(findId(Blocks.COMMAND_BLOCK))
            .addElement(findId(Blocks.REPEATING_COMMAND_BLOCK))
            .addElement(findId(Blocks.CHAIN_COMMAND_BLOCK))
            .addElement(findId(Blocks.BARRIER))
            .addElement(findId(Blocks.STRUCTURE_BLOCK))
            .addElement(findId(Blocks.JIGSAW))
            .addElement(ModBlocks.END_DUST.getId())
            .addElement(ModBlocks.NEGATIVE_MATTER_BLOCK.getId());

        provider.rawBuilder(ModBlockTags.VOID_DECAY_PRODUCTS)
            .addElement(findId(Blocks.STONE))
            .addElement(findId(Blocks.DEEPSLATE))
            .addElement(findId(Blocks.ANDESITE))
            .addElement(findId(Blocks.GRANITE))
            .addElement(findId(Blocks.DIORITE))
            .addElement(findId(Blocks.NETHERRACK))
            .addElement(findId(Blocks.BLACKSTONE))
            .addElement(findId(Blocks.END_STONE))
            .addElement(findId(Blocks.ICE))
            .addElement(findId(Blocks.RAW_IRON_BLOCK))
            .addElement(findId(Blocks.OXIDIZED_COPPER))
            .addElement(findId(Blocks.IRON_ORE))
            .addElement(findId(Blocks.DEEPSLATE_IRON_ORE))
            .addElement(findId(Blocks.COPPER_ORE))
            .addElement(findId(Blocks.DEEPSLATE_COPPER_ORE))
            .addElement(findId(Blocks.GOLD_ORE))
            .addElement(findId(Blocks.DEEPSLATE_GOLD_ORE))
            .addElement(findId(Blocks.DIRT))
            .addElement(findId(Blocks.COARSE_DIRT))
            .addElement(findId(Blocks.ROOTED_DIRT))
            .addElement(findId(Blocks.MUD))
            .addElement(findId(Blocks.CLAY))
            .addElement(findId(Blocks.COBBLESTONE))
            .addElement(findId(Blocks.MOSSY_COBBLESTONE))
            .addElement(findId(Blocks.CALCITE))
            .addElement(findId(Blocks.TUFF))
            .addElement(findId(Blocks.DRIPSTONE_BLOCK))
            .addElement(findId(Blocks.SANDSTONE))
            .addElement(findId(Blocks.RED_SANDSTONE))
            .addElement(findId(Blocks.BASALT))
            .addElement(findId(Blocks.SMOOTH_BASALT))
            .addElement(findId(Blocks.SCULK))
            .addElement(findId(Blocks.MOSS_BLOCK))
            .addElement(findId(Blocks.INFESTED_COBBLESTONE))
            .addElement(findId(Blocks.INFESTED_STONE))
            .addElement(findId(Blocks.INFESTED_DEEPSLATE))
            .addElement(findId(Blocks.NETHER_GOLD_ORE))
            .addElement(findId(Blocks.GILDED_BLACKSTONE))
            .addElement(findId(Blocks.NETHER_QUARTZ_ORE));

        provider.rawBuilder(ModBlockTags.CRAFTING_MATRIX_ELEMENT)
            .addElement(ModBlocks.SPACE_OVERCOMPRESSOR.getId())
            .addTag(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES.location());

        // mekanism integration
        provider.rawBuilder(ModBlockTags.MEKANISM_CARDBOARD_BOX_BLACKLIST)
            .addElement(ModBlocks.GIANT_ANVIL.getId())
            .addElement(ModBlocks.TRANSMISSION_POLE.getId())
            .addElement(ModBlocks.REMOTE_TRANSMISSION_POLE.getId())
            .addElement(ModBlocks.TESLA_TOWER.getId())
            .addElement(ModBlocks.OVERSEER.getId())
            .addElement(ModBlocks.ACCELERATION_RING.getId())
            .addElement(ModBlocks.DEFLECTION_RING.getId());

        provider.rawBuilder(ModBlockTags.ANVIL_HAMMER_BLACKLIST)
            .addElement(findId(Blocks.NETHER_PORTAL))
            .addElement(findId(Blocks.PISTON_HEAD))
            .addElement(findId(Blocks.END_PORTAL_FRAME))
            .addElement(findId(Blocks.ATTACHED_MELON_STEM))
            .addElement(findId(Blocks.ATTACHED_PUMPKIN_STEM))
            .addTag(BlockTags.BEDS.location())
            .addTag(BlockTags.ALL_SIGNS.location())
            .addTag(Tags.Blocks.CHESTS.location())
            .addTag(Tags.Blocks.CHESTS_ENDER.location())
            .addTag(Tags.Blocks.CHESTS_TRAPPED.location())
            .addTag(Tags.Blocks.CHESTS_WOODEN.location());
        provider.rawBuilder(ModBlockTags.DEVOUR_BLACKLIST)
            .addTag(Tags.Blocks.CHESTS_TRAPPED.location())
            .addTag(Tags.Blocks.CHESTS_WOODEN.location());

        provider.rawBuilder(ModBlockTags.FELLING_APPLICABLE)
            .addTag(BlockTags.LOGS.location())
            .addTag(BlockTags.WART_BLOCKS.location())
            .addTag(BlockTags.BEEHIVES.location())
            .addTag(ModBlockTags.MUSHROOM_BLOCK.location())
            .addElement(findId(Blocks.MANGROVE_ROOTS))
            .addElement(findId(Blocks.SHROOMLIGHT))
            .addElement(findId(Blocks.MUSHROOM_STEM))
            .addElement(findId(Blocks.SUGAR_CANE))
            .addElement(findId(Blocks.BAMBOO_BLOCK))
            .addElement(findId(Blocks.CHORUS_PLANT))
            .addElement(findId(Blocks.CHORUS_FLOWER))
            .addElement(findId(Blocks.CACTUS))
            .addElement(findId(Blocks.KELP_PLANT))
            .addElement(findId(Blocks.BAMBOO))
            .addElement(findId(Blocks.BAMBOO_SAPLING));

        provider.rawBuilder(ModBlockTags.CLEANING_APPLICABLE)
            .addElement(findId(Blocks.GRASS_BLOCK))
            .addElement(findId(Blocks.TALL_GRASS))
            .addElement(findId(Blocks.SHORT_GRASS))
            .addElement(findId(Blocks.FERN))
            .addElement(findId(Blocks.LARGE_FERN))
            .addTag(BlockTags.FLOWERS.location())
            .addElement(findId(Blocks.DEAD_BUSH))
            .addElement(findId(Blocks.RED_MUSHROOM))
            .addElement(findId(Blocks.BROWN_MUSHROOM))
            .addElement(findId(Blocks.CRIMSON_FUNGUS))
            .addElement(findId(Blocks.WARPED_FUNGUS))
            .addElement(findId(Blocks.CRIMSON_ROOTS))
            .addElement(findId(Blocks.WARPED_ROOTS))
            .addElement(findId(Blocks.NETHER_SPROUTS))
            .addElement(findId(Blocks.SCULK_VEIN))
            .addElement(findId(Blocks.COBWEB))
            .addElement(findId(Blocks.GLOW_LICHEN))
            .addElement(findId(Blocks.VINE))
            .addElement(findId(Blocks.SNOW))
            .addElement(findId(Blocks.MOSS_CARPET))
            .addElement(findId(Blocks.LILY_PAD))
            .addElement(findId(Blocks.SEAGRASS))
            .addElement(findId(Blocks.TALL_SEAGRASS))
            .addElement(findId(Blocks.SEA_PICKLE))
            .addElement(findId(Blocks.KELP_PLANT))
            .addTag(BlockTags.WALL_CORALS.location())
            .addTag(BlockTags.CORAL_PLANTS.location());

        provider.rawBuilder(ModBlockTags.SPECTRAL_CAN_THROUGH)
            .addTag(Tags.Blocks.GLASS_BLOCKS.location())
            .addTag(Tags.Blocks.GLASS_PANES.location())
            .addTag(BlockTags.LEAVES.location())
            .addElement(findId(Blocks.IRON_BARS))
            .addElement(findId(Blocks.MANGROVE_ROOTS))
            .addElement(findId(Blocks.COPPER_GRATE));

        provider.rawBuilder(ModBlockTags.HEATABLE_BLOCKS)
            .addTag(ModBlockTags.STORAGE_BLOCKS_TUNGSTEN.location())
            .addElement(findId(Blocks.NETHERITE_BLOCK));

        provider.rawBuilder(ModBlockTags.STICKABLE_WITH_SLIDING_RAILS)
            .addTag(ModBlockTags.SLIDING_RAILS.location())
            .addElement(ModBlocks.SLIDING_RAIL_STOP.getId());

        provider.rawBuilder(ModBlockTags.OVERHEATABLE)
            .addElement(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.getId())
            .addElement(ModBlocks.EMBER_METAL_BLOCK.getId());

        // tier 0：原版三种铁砧以及下列所有;
        // tier 1：皇家铁砧以及下列所有;
        // tier 2：余烬铁砧以及下列所有;
        // tier 3：超限铁砧
        provider.rawBuilder(ModBlockTags.ANVIL_TIER_0)
            .addElement(findId(Blocks.ANVIL))
            .addElement(findId(Blocks.CHIPPED_ANVIL))
            .addElement(findId(Blocks.DAMAGED_ANVIL))
            .addTag(ModBlockTags.ANVIL_TIER_1.location());

        provider.rawBuilder(ModBlockTags.ANVIL_TIER_1)
            .addElement(ModBlocks.ROYAL_ANVIL.getId())
            .addTag(ModBlockTags.ANVIL_TIER_2.location());

        provider.rawBuilder(ModBlockTags.ANVIL_TIER_2)
            .addElement(ModBlocks.FROST_ANVIL.getId())
            .addElement(ModBlocks.EMBER_ANVIL.getId())
            .addTag(ModBlockTags.ANVIL_TIER_3.location());

        provider.rawBuilder(ModBlockTags.ANVIL_TIER_3)
            .addElement(ModBlocks.TRANSCENDENCE_ANVIL.getId());

        provider.rawBuilder(ModBlockTags.NEEDS_NETHERITE_TOOL);
        provider.rawBuilder(ModBlockTags.NEEDS_EMBER_TOOL);
        provider.rawBuilder(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL);

        provider.rawBuilder(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
            .addTag(ModBlockTags.NEEDS_NETHERITE_TOOL.location())
            .addTag(ModBlockTags.NEEDS_EMBER_TOOL.location())
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(BlockTags.INCORRECT_FOR_STONE_TOOL)
            .addTag(ModBlockTags.NEEDS_NETHERITE_TOOL.location())
            .addTag(ModBlockTags.NEEDS_EMBER_TOOL.location())
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(BlockTags.INCORRECT_FOR_IRON_TOOL)
            .addTag(ModBlockTags.NEEDS_NETHERITE_TOOL.location())
            .addTag(ModBlockTags.NEEDS_EMBER_TOOL.location())
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(BlockTags.INCORRECT_FOR_GOLD_TOOL)
            .addTag(ModBlockTags.NEEDS_NETHERITE_TOOL.location())
            .addTag(ModBlockTags.NEEDS_EMBER_TOOL.location())
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
            .addTag(ModBlockTags.NEEDS_NETHERITE_TOOL.location())
            .addTag(ModBlockTags.NEEDS_EMBER_TOOL.location())
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)
            .addTag(ModBlockTags.NEEDS_EMBER_TOOL.location())
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(ModBlockTags.INCORRECT_FOR_EMBER_TOOL)
            .addTag(ModBlockTags.NEEDS_TRANSCENDIUM_TOOL.location());

        provider.rawBuilder(ModBlockTags.INCORRECT_FOR_TRANSCENDIUM_TOOL);
    }
}
