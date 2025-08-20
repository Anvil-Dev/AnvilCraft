package dev.dubhe.anvilcraft.data.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

public class BlockTagLoader {

    private static ResourceKey<Block> findResourceKey(Block item) {
        return ResourceKey.create(Registries.BLOCK, BuiltInRegistries.BLOCK.getKey(item));
    }

    /**
     * 初始化方块标签
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<Block> provider) {
        provider.addTag(ModBlockTags.REDSTONE_TORCH)
            .add(findResourceKey(Blocks.REDSTONE_WALL_TORCH))
            .add(findResourceKey(Blocks.REDSTONE_TORCH));

        provider.addTag(ModBlockTags.MUSHROOM_BLOCK)
            .add(findResourceKey(Blocks.BROWN_MUSHROOM_BLOCK))
            .add(findResourceKey(Blocks.RED_MUSHROOM_BLOCK))
            .add(findResourceKey(Blocks.MUSHROOM_STEM));

        provider.addTag(ModBlockTags.HAMMER_CHANGEABLE)
            .add(findResourceKey(Blocks.OBSERVER))
            .add(findResourceKey(Blocks.HOPPER))
            .add(findResourceKey(Blocks.DROPPER))
            .add(findResourceKey(Blocks.DISPENSER))
            .add(findResourceKey(Blocks.CRAFTER))
            .add(findResourceKey(Blocks.LIGHTNING_ROD));

        provider.addTag(ModBlockTags.HAMMER_REMOVABLE)
            .addTag(BlockTags.TRAPDOORS)
            .addTag(BlockTags.DOORS)
            .addTag(BlockTags.BUTTONS)
            .addTag(BlockTags.PRESSURE_PLATES)
            .add(findResourceKey(Blocks.BELL))
            .add(findResourceKey(Blocks.REDSTONE_LAMP))
            .add(findResourceKey(Blocks.RAIL))
            .add(findResourceKey(Blocks.ACTIVATOR_RAIL))
            .add(findResourceKey(Blocks.DETECTOR_RAIL))
            .add(findResourceKey(Blocks.POWERED_RAIL))
            .add(findResourceKey(Blocks.NOTE_BLOCK))
            .add(findResourceKey(Blocks.OBSERVER))
            .add(findResourceKey(Blocks.HOPPER))
            .add(findResourceKey(Blocks.DROPPER))
            .add(findResourceKey(Blocks.DISPENSER))
            .add(findResourceKey(Blocks.CRAFTER))
            .add(findResourceKey(Blocks.HONEY_BLOCK))
            .add(findResourceKey(Blocks.SLIME_BLOCK))
            .add(findResourceKey(Blocks.PISTON))
            .add(findResourceKey(Blocks.STICKY_PISTON))
            .add(findResourceKey(Blocks.PISTON_HEAD))
            .add(findResourceKey(Blocks.LIGHTNING_ROD))
            .add(findResourceKey(Blocks.DAYLIGHT_DETECTOR))
            .add(findResourceKey(Blocks.LECTERN))
            .add(findResourceKey(Blocks.TRIPWIRE_HOOK))
            .add(findResourceKey(Blocks.SCULK_SHRIEKER))
            .add(findResourceKey(Blocks.LEVER))
            .add(findResourceKey(Blocks.SCULK_SENSOR))
            .add(findResourceKey(Blocks.CALIBRATED_SCULK_SENSOR))
            .add(findResourceKey(Blocks.REDSTONE_WIRE))
            .add(findResourceKey(Blocks.REDSTONE_TORCH))
            .add(findResourceKey(Blocks.REDSTONE_WALL_TORCH))
            .add(findResourceKey(Blocks.REDSTONE_BLOCK))
            .add(findResourceKey(Blocks.REPEATER))
            .add(findResourceKey(Blocks.COMPARATOR))
            .add(findResourceKey(Blocks.TARGET))
            .add(findResourceKey(Blocks.COPPER_BULB))
            .add(findResourceKey(Blocks.EXPOSED_COPPER_BULB))
            .add(findResourceKey(Blocks.WEATHERED_COPPER_BULB))
            .add(findResourceKey(Blocks.OXIDIZED_COPPER_BULB))
            .add(findResourceKey(Blocks.WAXED_COPPER_BULB))
            .add(findResourceKey(Blocks.WAXED_EXPOSED_COPPER_BULB))
            .add(findResourceKey(Blocks.WAXED_WEATHERED_COPPER_BULB))
            .add(findResourceKey(Blocks.WAXED_OXIDIZED_COPPER_BULB))
            .add(findResourceKey(Blocks.CAULDRON))
            .add(findResourceKey(Blocks.LAVA_CAULDRON))
            .add(findResourceKey(Blocks.WATER_CAULDRON))
            .add(findResourceKey(Blocks.POWDER_SNOW_CAULDRON))
            .add(findResourceKey(Blocks.CAMPFIRE))
            .add(findResourceKey(Blocks.STONECUTTER))
            .add(findResourceKey(Blocks.SCAFFOLDING))
            .add(findResourceKey(Blocks.ANVIL))
            .add(findResourceKey(Blocks.CHIPPED_ANVIL))
            .add(findResourceKey(Blocks.DAMAGED_ANVIL))
            .add(ModBlocks.HEAVY_IRON_BLOCK.getKey())
            .add(ModBlocks.HEAVY_IRON_BEAM.getKey())
            .add(ModBlocks.HEAVY_IRON_COLUMN.getKey())
            .add(ModBlocks.HEAVY_IRON_PLATE.getKey())
            .add(ModBlocks.CUT_HEAVY_IRON_BLOCK.getKey())
            .add(ModBlocks.CUT_HEAVY_IRON_SLAB.getKey())
            .add(ModBlocks.CUT_HEAVY_IRON_STAIRS.getKey())
            .add(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.getKey())
            .add(ModBlocks.POLISHED_HEAVY_IRON_SLAB.getKey())
            .add(ModBlocks.POLISHED_HEAVY_IRON_STAIRS.getKey());

        provider.addTag(ModBlockTags.UNDER_CAULDRON)
            .addTag(BlockTags.CAMPFIRES)
            .add(findResourceKey(Blocks.MAGMA_BLOCK))
            .add(ModBlocks.HEATER.getKey())
            .add(ModBlocks.CORRUPTED_BEACON.getKey());

        provider.addTag(ModBlockTags.BLOCK_DEVOURER_CHAIN_DEVOURING)
            .addTag(Tags.Blocks.SANDS)
            .addTag(Tags.Blocks.GRAVELS);

        provider.addTag(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
            .add(findResourceKey(Blocks.STONE))
            .add(findResourceKey(Blocks.DEEPSLATE))
            .add(findResourceKey(Blocks.ANDESITE))
            .add(findResourceKey(Blocks.DIORITE))
            .add(findResourceKey(Blocks.GRANITE))
            .add(findResourceKey(Blocks.TUFF))
            .add(findResourceKey(Blocks.NETHERRACK))
            .add(findResourceKey(Blocks.BASALT))
            .add(findResourceKey(Blocks.BLACKSTONE))
            .add(findResourceKey(Blocks.END_STONE));

        provider.addTag(ModBlockTags.LASER_CAN_PASS_THROUGH)
            .addTag(Tags.Blocks.GLASS_BLOCKS)
            .addTag(Tags.Blocks.GLASS_PANES)
            .addTag(BlockTags.REPLACEABLE);

        provider.addTag(ModBlockTags.END_PORTAL_UNABLE_CHANGE).add(findResourceKey(Blocks.DRAGON_EGG));

        provider.addTag(ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)
            .add(findResourceKey(Blocks.END_STONE))
            .add(findResourceKey(Blocks.BEDROCK))
            .add(findResourceKey(Blocks.COMMAND_BLOCK))
            .add(findResourceKey(Blocks.REPEATING_COMMAND_BLOCK))
            .add(findResourceKey(Blocks.CHAIN_COMMAND_BLOCK))
            .add(findResourceKey(Blocks.BARRIER))
            .add(findResourceKey(Blocks.STRUCTURE_BLOCK))
            .add(findResourceKey(Blocks.JIGSAW))
            .add(ModBlocks.END_DUST.getKey())
            .add(ModBlocks.NEGATIVE_MATTER_BLOCK.getKey());

        provider.addTag(ModBlockTags.VOID_DECAY_PRODUCTS)
            .add(ModBlocks.FLINT_BLOCK.getKey())
            .add(findResourceKey(Blocks.STONE))
            .add(findResourceKey(Blocks.DEEPSLATE))
            .add(findResourceKey(Blocks.ANDESITE))
            .add(findResourceKey(Blocks.GRANITE))
            .add(findResourceKey(Blocks.DIORITE))
            .add(findResourceKey(Blocks.NETHERRACK))
            .add(findResourceKey(Blocks.BLACKSTONE))
            .add(findResourceKey(Blocks.END_STONE))
            .add(findResourceKey(Blocks.ICE))
            .add(findResourceKey(Blocks.RAW_IRON_BLOCK))
            .add(findResourceKey(Blocks.OXIDIZED_COPPER))
            .add(findResourceKey(Blocks.IRON_ORE))
            .add(findResourceKey(Blocks.DEEPSLATE_IRON_ORE))
            .add(findResourceKey(Blocks.COPPER_ORE))
            .add(findResourceKey(Blocks.DEEPSLATE_COPPER_ORE))
            .add(findResourceKey(Blocks.GOLD_ORE))
            .add(findResourceKey(Blocks.DEEPSLATE_GOLD_ORE))
            .add(findResourceKey(Blocks.DIRT))
            .add(findResourceKey(Blocks.COARSE_DIRT))
            .add(findResourceKey(Blocks.ROOTED_DIRT))
            .add(findResourceKey(Blocks.MUD))
            .add(findResourceKey(Blocks.CLAY))
            .add(findResourceKey(Blocks.COBBLESTONE))
            .add(findResourceKey(Blocks.MOSSY_COBBLESTONE))
            .add(findResourceKey(Blocks.CALCITE))
            .add(findResourceKey(Blocks.TUFF))
            .add(findResourceKey(Blocks.DRIPSTONE_BLOCK))
            .add(findResourceKey(Blocks.SANDSTONE))
            .add(findResourceKey(Blocks.RED_SANDSTONE))
            .add(findResourceKey(Blocks.BASALT))
            .add(findResourceKey(Blocks.SMOOTH_BASALT))
            .add(findResourceKey(Blocks.SCULK))
            .add(findResourceKey(Blocks.MOSS_BLOCK))
            .add(findResourceKey(Blocks.INFESTED_COBBLESTONE))
            .add(findResourceKey(Blocks.INFESTED_STONE))
            .add(findResourceKey(Blocks.INFESTED_DEEPSLATE))
            .add(findResourceKey(Blocks.NETHER_GOLD_ORE))
            .add(findResourceKey(Blocks.GILDED_BLACKSTONE))
            .add(findResourceKey(Blocks.NETHER_QUARTZ_ORE))
            .add(ModBlocks.VOID_STONE.getKey())
            .add(ModBlocks.END_DUST.getKey())
            .add(ModBlocks.DEEPSLATE_TIN_ORE.getKey())
            .add(ModBlocks.DEEPSLATE_ZINC_ORE.getKey())
            .add(ModBlocks.DEEPSLATE_LEAD_ORE.getKey());

        provider.addTag(ModBlockTags.CRAFTING_MATRIX_ELEMENT)
            .add(ModBlocks.SPACE_OVERCOMPRESSOR.getKey())
            .addTag(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES);

        //mekanism integration
        provider.addTag(ModBlockTags.MEKANISM_CARDBOARD_BOX_BLACKLIST)
            .add(ModBlocks.GIANT_ANVIL.getKey())
            .add(ModBlocks.TRANSMISSION_POLE.getKey())
            .add(ModBlocks.REMOTE_TRANSMISSION_POLE.getKey())
            .add(ModBlocks.TESLA_TOWER.getKey())
            .add(ModBlocks.OVERSEER_BLOCK.getKey())
            .add(ModBlocks.ACCELERATION_RING.getKey())
            .add(ModBlocks.DEFLECTION_RING.getKey());

        provider.addTag(ModBlockTags.ANVIL_HAMMER_BLACKLIST)
            .add(ModBlocks.DEFLECTION_RING.getKey())
            .add(findResourceKey(Blocks.NETHER_PORTAL))
            .add(findResourceKey(Blocks.PISTON_HEAD))
            .add(findResourceKey(Blocks.END_PORTAL_FRAME))
            .add(findResourceKey(Blocks.ATTACHED_MELON_STEM))
            .add(findResourceKey(Blocks.ATTACHED_PUMPKIN_STEM))
            .addTag(BlockTags.BEDS)
            .addTag(BlockTags.ALL_SIGNS)
            .addTag(Tags.Blocks.CHESTS)
            .addTag(Tags.Blocks.CHESTS_ENDER)
            .addTag(Tags.Blocks.CHESTS_TRAPPED)
            .addTag(Tags.Blocks.CHESTS_WOODEN);
        provider.addTag(ModBlockTags.FELLING_APPLICABLE)
            .addTag(BlockTags.LOGS)
            .addTag(BlockTags.WART_BLOCKS)
            .addTag(BlockTags.BEEHIVES)
            .addTag(ModBlockTags.MUSHROOM_BLOCK)
            .add(findResourceKey(Blocks.MANGROVE_ROOTS))
            .add(findResourceKey(Blocks.SHROOMLIGHT))
            .add(findResourceKey(Blocks.MUSHROOM_STEM))
            .add(findResourceKey(Blocks.SUGAR_CANE))
            .add(findResourceKey(Blocks.BAMBOO_BLOCK))
            .add(findResourceKey(Blocks.CHORUS_PLANT))
            .add(findResourceKey(Blocks.CHORUS_FLOWER))
            .add(findResourceKey(Blocks.CACTUS))
            .add(findResourceKey(Blocks.KELP_PLANT))
            .add(findResourceKey(Blocks.BAMBOO))
            .add(findResourceKey(Blocks.BAMBOO_SAPLING));

        provider.addTag(ModBlockTags.CLEANING_APPLICABLE)
            .add(findResourceKey(Blocks.GRASS_BLOCK))
            .add(findResourceKey(Blocks.TALL_GRASS))
            .add(findResourceKey(Blocks.SHORT_GRASS))
            .add(findResourceKey(Blocks.FERN))
            .add(findResourceKey(Blocks.LARGE_FERN))
            .addTag(BlockTags.FLOWERS)
            .add(findResourceKey(Blocks.DEAD_BUSH))
            .add(findResourceKey(Blocks.RED_MUSHROOM))
            .add(findResourceKey(Blocks.BROWN_MUSHROOM))
            .add(findResourceKey(Blocks.CRIMSON_FUNGUS))
            .add(findResourceKey(Blocks.WARPED_FUNGUS))
            .add(findResourceKey(Blocks.CRIMSON_ROOTS))
            .add(findResourceKey(Blocks.WARPED_ROOTS))
            .add(findResourceKey(Blocks.NETHER_SPROUTS))
            .add(findResourceKey(Blocks.SCULK_VEIN))
            .add(findResourceKey(Blocks.COBWEB))
            .add(findResourceKey(Blocks.GLOW_LICHEN))
            .add(findResourceKey(Blocks.VINE))
            .add(findResourceKey(Blocks.SNOW))
            .add(findResourceKey(Blocks.MOSS_CARPET))
            .add(findResourceKey(Blocks.LILY_PAD));

        provider.addTag(ModBlockTags.SPECTRAL_CAN_THROUGH)
            .addTag(Tags.Blocks.GLASS_BLOCKS)
            .addTag(Tags.Blocks.GLASS_PANES)
            .addTag(BlockTags.LEAVES);

        provider.addTag(ModBlockTags.HEATABLE_BLOCKS)
            .add(findResourceKey(Blocks.NETHERITE_BLOCK));

        provider.addTag(ModBlockTags.STICKABLE_WITH_SLIDING_RAILS)
            .addTag(ModBlockTags.SLIDING_RAILS)
            .add(ModBlocks.SLIDING_RAIL_STOP.getKey());

        provider.addTag(ModBlockTags.OVERHEATABLE)
            .add(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.getKey())
            .add(ModBlocks.EMBER_METAL_BLOCK.getKey());
    }
}
