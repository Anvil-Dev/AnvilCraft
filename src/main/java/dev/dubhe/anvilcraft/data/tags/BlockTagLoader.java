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
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_WALL_TORCH))
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_TORCH));

        provider.rawBuilder(ModBlockTags.MUSHROOM_BLOCK)
            .addElement(BlockTagLoader.findId(Blocks.BROWN_MUSHROOM_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.RED_MUSHROOM_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.MUSHROOM_STEM));

        provider.rawBuilder(ModBlockTags.HAMMER_CHANGEABLE)
            .addElement(BlockTagLoader.findId(Blocks.OBSERVER))
            .addElement(BlockTagLoader.findId(Blocks.HOPPER))
            .addElement(BlockTagLoader.findId(Blocks.DROPPER))
            .addElement(BlockTagLoader.findId(Blocks.DISPENSER))
            .addElement(BlockTagLoader.findId(Blocks.CRAFTER))
            .addElement(BlockTagLoader.findId(Blocks.LIGHTNING_ROD));

        provider.rawBuilder(ModBlockTags.HAMMER_REMOVABLE)
            .addTag(BlockTags.TRAPDOORS.location())
            .addTag(BlockTags.DOORS.location())
            .addTag(BlockTags.BUTTONS.location())
            .addTag(BlockTags.PRESSURE_PLATES.location())
            .addTag(BlockTags.FENCE_GATES.location())
            .addElement(BlockTagLoader.findId(Blocks.BELL))
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_LAMP))
            .addElement(BlockTagLoader.findId(Blocks.RAIL))
            .addElement(BlockTagLoader.findId(Blocks.ACTIVATOR_RAIL))
            .addElement(BlockTagLoader.findId(Blocks.DETECTOR_RAIL))
            .addElement(BlockTagLoader.findId(Blocks.POWERED_RAIL))
            .addElement(BlockTagLoader.findId(Blocks.NOTE_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.OBSERVER))
            .addElement(BlockTagLoader.findId(Blocks.HOPPER))
            .addElement(BlockTagLoader.findId(Blocks.DROPPER))
            .addElement(BlockTagLoader.findId(Blocks.DISPENSER))
            .addElement(BlockTagLoader.findId(Blocks.CRAFTER))
            .addElement(BlockTagLoader.findId(Blocks.HONEY_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.SLIME_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.PISTON))
            .addElement(BlockTagLoader.findId(Blocks.STICKY_PISTON))
            .addElement(BlockTagLoader.findId(Blocks.PISTON_HEAD))
            .addElement(BlockTagLoader.findId(Blocks.LIGHTNING_ROD))
            .addElement(BlockTagLoader.findId(Blocks.DAYLIGHT_DETECTOR))
            .addElement(BlockTagLoader.findId(Blocks.LECTERN))
            .addElement(BlockTagLoader.findId(Blocks.TRIPWIRE_HOOK))
            .addElement(BlockTagLoader.findId(Blocks.SCULK_SHRIEKER))
            .addElement(BlockTagLoader.findId(Blocks.LEVER))
            .addElement(BlockTagLoader.findId(Blocks.SCULK_SENSOR))
            .addElement(BlockTagLoader.findId(Blocks.CALIBRATED_SCULK_SENSOR))
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_WIRE))
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_TORCH))
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_WALL_TORCH))
            .addElement(BlockTagLoader.findId(Blocks.REDSTONE_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.REPEATER))
            .addElement(BlockTagLoader.findId(Blocks.COMPARATOR))
            .addElement(BlockTagLoader.findId(Blocks.TARGET))
            .addElement(BlockTagLoader.findId(Blocks.COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.EXPOSED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.WEATHERED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.OXIDIZED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.WAXED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.WAXED_EXPOSED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.WAXED_WEATHERED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.WAXED_OXIDIZED_COPPER_BULB))
            .addElement(BlockTagLoader.findId(Blocks.CAULDRON))
            .addElement(BlockTagLoader.findId(Blocks.LAVA_CAULDRON))
            .addElement(BlockTagLoader.findId(Blocks.WATER_CAULDRON))
            .addElement(BlockTagLoader.findId(Blocks.POWDER_SNOW_CAULDRON))
            .addElement(BlockTagLoader.findId(Blocks.CAMPFIRE))
            .addElement(BlockTagLoader.findId(Blocks.STONECUTTER))
            .addElement(BlockTagLoader.findId(Blocks.SCAFFOLDING))
            .addElement(BlockTagLoader.findId(Blocks.ANVIL))
            .addElement(BlockTagLoader.findId(Blocks.CHIPPED_ANVIL))
            .addElement(BlockTagLoader.findId(Blocks.DAMAGED_ANVIL))
            .addElement(BlockTagLoader.findId(Blocks.FURNACE))
            .addElement(BlockTagLoader.findId(Blocks.BLAST_FURNACE))
            .addElement(BlockTagLoader.findId(Blocks.SMOKER))
            .addElement(BlockTagLoader.findId(Blocks.CHEST))
            .addElement(BlockTagLoader.findId(Blocks.TRAPPED_CHEST))
            .addElement(BlockTagLoader.findId(Blocks.ENDER_CHEST))
            .addElement(BlockTagLoader.findId(Blocks.BARREL))
            .addElement(BlockTagLoader.findId(Blocks.COMPOSTER))
            .addElement(BlockTagLoader.findId(Blocks.TNT))
            .addElement(BlockTagLoader.findId(Blocks.BEACON))
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
            .addElement(BlockTagLoader.findId(Blocks.MAGMA_BLOCK))
            .addElement(ModBlocks.HEATER.getId())
            .addElement(ModBlocks.CORRUPTED_BEACON.getId());

        provider.rawBuilder(ModBlockTags.BLOCK_DEVOURER_CHAIN_DEVOURING)
            .addTag(Tags.Blocks.SANDS.location())
            .addTag(Tags.Blocks.GRAVELS.location());

        provider.rawBuilder(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
            .addElement(BlockTagLoader.findId(Blocks.STONE))
            .addElement(BlockTagLoader.findId(Blocks.DEEPSLATE))
            .addElement(BlockTagLoader.findId(Blocks.ANDESITE))
            .addElement(BlockTagLoader.findId(Blocks.DIORITE))
            .addElement(BlockTagLoader.findId(Blocks.GRANITE))
            .addElement(BlockTagLoader.findId(Blocks.TUFF))
            .addElement(BlockTagLoader.findId(Blocks.NETHERRACK))
            .addElement(BlockTagLoader.findId(Blocks.BASALT))
            .addElement(BlockTagLoader.findId(Blocks.BLACKSTONE))
            .addElement(BlockTagLoader.findId(Blocks.END_STONE));

        provider.rawBuilder(ModBlockTags.LASER_CAN_PASS_THROUGH)
            .addTag(Tags.Blocks.GLASS_BLOCKS.location())
            .addTag(Tags.Blocks.GLASS_PANES.location())
            .addTag(BlockTags.REPLACEABLE.location());

        provider.rawBuilder(ModBlockTags.END_PORTAL_UNABLE_CHANGE)
            .addElement(BlockTagLoader.findId(Blocks.DRAGON_EGG));

        provider.rawBuilder(ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)
            .addElement(BlockTagLoader.findId(Blocks.END_STONE))
            .addElement(BlockTagLoader.findId(Blocks.BEDROCK))
            .addElement(BlockTagLoader.findId(Blocks.COMMAND_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.REPEATING_COMMAND_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.CHAIN_COMMAND_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.BARRIER))
            .addElement(BlockTagLoader.findId(Blocks.STRUCTURE_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.JIGSAW))
            .addElement(ModBlocks.END_DUST.getId())
            .addElement(ModBlocks.NEGATIVE_MATTER_BLOCK.getId());

        provider.rawBuilder(ModBlockTags.VOID_DECAY_PRODUCTS)
            .addElement(BlockTagLoader.findId(Blocks.STONE))
            .addElement(BlockTagLoader.findId(Blocks.DEEPSLATE))
            .addElement(BlockTagLoader.findId(Blocks.ANDESITE))
            .addElement(BlockTagLoader.findId(Blocks.GRANITE))
            .addElement(BlockTagLoader.findId(Blocks.DIORITE))
            .addElement(BlockTagLoader.findId(Blocks.NETHERRACK))
            .addElement(BlockTagLoader.findId(Blocks.BLACKSTONE))
            .addElement(BlockTagLoader.findId(Blocks.END_STONE))
            .addElement(BlockTagLoader.findId(Blocks.ICE))
            .addElement(BlockTagLoader.findId(Blocks.RAW_IRON_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.OXIDIZED_COPPER))
            .addElement(BlockTagLoader.findId(Blocks.IRON_ORE))
            .addElement(BlockTagLoader.findId(Blocks.DEEPSLATE_IRON_ORE))
            .addElement(BlockTagLoader.findId(Blocks.COPPER_ORE))
            .addElement(BlockTagLoader.findId(Blocks.DEEPSLATE_COPPER_ORE))
            .addElement(BlockTagLoader.findId(Blocks.GOLD_ORE))
            .addElement(BlockTagLoader.findId(Blocks.DEEPSLATE_GOLD_ORE))
            .addElement(BlockTagLoader.findId(Blocks.DIRT))
            .addElement(BlockTagLoader.findId(Blocks.COARSE_DIRT))
            .addElement(BlockTagLoader.findId(Blocks.ROOTED_DIRT))
            .addElement(BlockTagLoader.findId(Blocks.MUD))
            .addElement(BlockTagLoader.findId(Blocks.CLAY))
            .addElement(BlockTagLoader.findId(Blocks.COBBLESTONE))
            .addElement(BlockTagLoader.findId(Blocks.MOSSY_COBBLESTONE))
            .addElement(BlockTagLoader.findId(Blocks.CALCITE))
            .addElement(BlockTagLoader.findId(Blocks.TUFF))
            .addElement(BlockTagLoader.findId(Blocks.DRIPSTONE_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.SANDSTONE))
            .addElement(BlockTagLoader.findId(Blocks.RED_SANDSTONE))
            .addElement(BlockTagLoader.findId(Blocks.BASALT))
            .addElement(BlockTagLoader.findId(Blocks.SMOOTH_BASALT))
            .addElement(BlockTagLoader.findId(Blocks.SCULK))
            .addElement(BlockTagLoader.findId(Blocks.MOSS_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.INFESTED_COBBLESTONE))
            .addElement(BlockTagLoader.findId(Blocks.INFESTED_STONE))
            .addElement(BlockTagLoader.findId(Blocks.INFESTED_DEEPSLATE))
            .addElement(BlockTagLoader.findId(Blocks.NETHER_GOLD_ORE))
            .addElement(BlockTagLoader.findId(Blocks.GILDED_BLACKSTONE))
            .addElement(BlockTagLoader.findId(Blocks.NETHER_QUARTZ_ORE));

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
            .addElement(BlockTagLoader.findId(Blocks.NETHER_PORTAL))
            .addElement(BlockTagLoader.findId(Blocks.PISTON_HEAD))
            .addElement(BlockTagLoader.findId(Blocks.END_PORTAL_FRAME))
            .addElement(BlockTagLoader.findId(Blocks.ATTACHED_MELON_STEM))
            .addElement(BlockTagLoader.findId(Blocks.ATTACHED_PUMPKIN_STEM))
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_INTERFACE_PLACEHOLDER.getId())
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
            .addElement(BlockTagLoader.findId(Blocks.MANGROVE_ROOTS))
            .addElement(BlockTagLoader.findId(Blocks.SHROOMLIGHT))
            .addElement(BlockTagLoader.findId(Blocks.MUSHROOM_STEM))
            .addElement(BlockTagLoader.findId(Blocks.SUGAR_CANE))
            .addElement(BlockTagLoader.findId(Blocks.BAMBOO_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.CHORUS_PLANT))
            .addElement(BlockTagLoader.findId(Blocks.CHORUS_FLOWER))
            .addElement(BlockTagLoader.findId(Blocks.CACTUS))
            .addElement(BlockTagLoader.findId(Blocks.KELP_PLANT))
            .addElement(BlockTagLoader.findId(Blocks.BAMBOO))
            .addElement(BlockTagLoader.findId(Blocks.BAMBOO_SAPLING));

        provider.rawBuilder(ModBlockTags.CLEANING_APPLICABLE)
            .addElement(BlockTagLoader.findId(Blocks.GRASS_BLOCK))
            .addElement(BlockTagLoader.findId(Blocks.TALL_GRASS))
            .addElement(BlockTagLoader.findId(Blocks.SHORT_GRASS))
            .addElement(BlockTagLoader.findId(Blocks.FERN))
            .addElement(BlockTagLoader.findId(Blocks.LARGE_FERN))
            .addTag(BlockTags.FLOWERS.location())
            .addElement(BlockTagLoader.findId(Blocks.DEAD_BUSH))
            .addElement(BlockTagLoader.findId(Blocks.RED_MUSHROOM))
            .addElement(BlockTagLoader.findId(Blocks.BROWN_MUSHROOM))
            .addElement(BlockTagLoader.findId(Blocks.CRIMSON_FUNGUS))
            .addElement(BlockTagLoader.findId(Blocks.WARPED_FUNGUS))
            .addElement(BlockTagLoader.findId(Blocks.CRIMSON_ROOTS))
            .addElement(BlockTagLoader.findId(Blocks.WARPED_ROOTS))
            .addElement(BlockTagLoader.findId(Blocks.NETHER_SPROUTS))
            .addElement(BlockTagLoader.findId(Blocks.SCULK_VEIN))
            .addElement(BlockTagLoader.findId(Blocks.COBWEB))
            .addElement(BlockTagLoader.findId(Blocks.GLOW_LICHEN))
            .addElement(BlockTagLoader.findId(Blocks.VINE))
            .addElement(BlockTagLoader.findId(Blocks.SNOW))
            .addElement(BlockTagLoader.findId(Blocks.MOSS_CARPET))
            .addElement(BlockTagLoader.findId(Blocks.LILY_PAD))
            .addElement(BlockTagLoader.findId(Blocks.SEAGRASS))
            .addElement(BlockTagLoader.findId(Blocks.TALL_SEAGRASS))
            .addElement(BlockTagLoader.findId(Blocks.SEA_PICKLE))
            .addElement(BlockTagLoader.findId(Blocks.KELP_PLANT))
            .addTag(BlockTags.WALL_CORALS.location())
            .addTag(BlockTags.CORAL_PLANTS.location());

        provider.rawBuilder(ModBlockTags.BROKEN_CRYSTALS_CLUSTERS)
            .addElement(BlockTagLoader.findId(Blocks.AMETHYST_CLUSTER));

        provider.rawBuilder(ModBlockTags.SPECTRAL_CAN_THROUGH)
            .addTag(Tags.Blocks.GLASS_BLOCKS.location())
            .addTag(Tags.Blocks.GLASS_PANES.location())
            .addTag(BlockTags.LEAVES.location())
            .addElement(BlockTagLoader.findId(Blocks.IRON_BARS))
            .addElement(BlockTagLoader.findId(Blocks.MANGROVE_ROOTS))
            .addElement(BlockTagLoader.findId(Blocks.COPPER_GRATE))
            .addOptionalTag(ModBlockTags.AE2_GLASS_CABLE.location())
            .addOptionalTag(ModBlockTags.AE2_COVERED_CABLE.location())
            .addOptionalTag(ModBlockTags.AE2_SMART_CABLE.location())
            .addOptionalTag(ModBlockTags.AE2_COVERED_DENSE_CABLE.location())
            .addOptionalTag(ModBlockTags.AE2_SMART_DENSE_CABLE.location());

        provider.rawBuilder(ModBlockTags.HEATABLE_BLOCKS)
            .addTag(ModBlockTags.STORAGE_BLOCKS_TUNGSTEN.location())
            .addElement(BlockTagLoader.findId(Blocks.NETHERITE_BLOCK));

        provider.rawBuilder(ModBlockTags.STICKABLE_WITH_SLIDING_RAILS)
            .addTag(ModBlockTags.SLIDING_RAILS.location())
            .addElement(ModBlocks.SLIDING_RAIL_STOP.getId());

        provider.rawBuilder(ModBlockTags.OVERHEATABLE)
            .addElement(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.getId())
            .addElement(ModBlocks.EMBER_METAL_BLOCK.getId());

        // tier 0：所有铁砧以及下列所有;
        // tier 1：皇家铁砧以及下列所有;
        // tier 2：余烬铁砧以及下列所有;
        // tier 3：超限铁砧
        provider.rawBuilder(ModBlockTags.ANVIL_TIER_0)
            .addTag(BlockTags.ANVIL.location())
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

        provider.rawBuilder(ModBlockTags.ROYAL_SERIES)
            .addElement(ModBlocks.ROYAL_STEEL_BLOCK.getId())
            .addElement(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK.getId())
            .addElement(ModBlocks.CUT_ROYAL_STEEL_BLOCK.getId())
            .addElement(ModBlocks.CUT_ROYAL_STEEL_PILLAR.getId())
            .addElement(ModBlocks.CUT_ROYAL_STEEL_SLAB.getId())
            .addElement(ModBlocks.CUT_ROYAL_STEEL_STAIRS.getId());

        provider.rawBuilder(ModBlockTags.FROST_SERIES)
            .addElement(ModBlocks.FROST_METAL_BLOCK.getId())
            .addElement(ModBlocks.CUT_FROST_METAL_BLOCK.getId())
            .addElement(ModBlocks.CUT_FROST_METAL_PILLAR.getId())
            .addElement(ModBlocks.CUT_FROST_METAL_SLAB.getId())
            .addElement(ModBlocks.CUT_FROST_METAL_STAIRS.getId());

        provider.rawBuilder(ModBlockTags.EMBER_SERIES)
            .addElement(ModBlocks.EMBER_METAL_BLOCK.getId())
            .addElement(ModBlocks.CUT_EMBER_METAL_BLOCK.getId())
            .addElement(ModBlocks.CUT_EMBER_METAL_PILLAR.getId())
            .addElement(ModBlocks.CUT_EMBER_METAL_SLAB.getId())
            .addElement(ModBlocks.CUT_EMBER_METAL_STAIRS.getId());

        provider.rawBuilder(ModBlockTags.OVERSEER_BASE_TIER_0)
            .addElement(findId(Blocks.IRON_BLOCK))
            .addElement(findId(Blocks.GOLD_BLOCK))
            .addElement(findId(Blocks.DIAMOND_BLOCK))
            .addElement(findId(Blocks.EMERALD_BLOCK));

        provider.rawBuilder(ModBlockTags.OVERSEER_BASE_TIER_1)
            .addTag(ModBlockTags.ROYAL_SERIES.location())
            .addTag(ModBlockTags.FROST_SERIES.location());

        provider.rawBuilder(ModBlockTags.OVERSEER_BASE_TIER_2)
            .addElement(findId(Blocks.NETHERITE_BLOCK))
            .addTag(ModBlockTags.EMBER_SERIES.location())
            .addElement(ModBlocks.MULTIPHASE_MATTER_BLOCK.getId());

        provider.rawBuilder(ModBlockTags.OVERSEER_BASE_TIER_3)
            .addElement(ModBlocks.TRANSCENDIUM_BLOCK.getId());

        provider.rawBuilder(ModBlockTags.SLIDING_RAIL_STOP_LIKE)
            .addTag(ModBlockTags.HEATABLE_BLOCKS.location())
            .addElement(BlockTagLoader.findId(Blocks.CAMPFIRE))
            .addElement(ModBlocks.SLIDING_RAIL_STOP.getId())
            .addElement(ModBlocks.HEATER.getId())
            .addElement(ModBlocks.BURNING_HEATER.getId())
            .addElement(ModBlocks.CORRUPTED_BEACON.getId())
            .addElement(ModBlocks.NEUTRON_IRRADIATOR.getId());

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

        // 锻星砧相关方块免疫凋灵和末影龙破坏。
        provider.rawBuilder(BlockTags.WITHER_IMMUNE)
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.getId());

        provider.rawBuilder(BlockTags.DRAGON_IMMUNE)
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.getId())
            .addElement(ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.getId());

        provider.rawBuilder(BlockTags.ENCHANTMENT_POWER_PROVIDER)
            .addElement(ModBlocks.TRANSCENDIUM_BLOCK.getId());
    }
}
