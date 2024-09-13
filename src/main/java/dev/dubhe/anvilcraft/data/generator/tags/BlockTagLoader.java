package dev.dubhe.anvilcraft.data.generator.tags;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
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
                .add(findResourceKey(Blocks.LIGHTNING_ROD));
        provider.addTag(ModBlockTags.HAMMER_REMOVABLE)
                .add(findResourceKey(Blocks.BELL))
                .add(findResourceKey(Blocks.REDSTONE_LAMP))
                .add(findResourceKey(Blocks.IRON_DOOR))
                .add(findResourceKey(Blocks.RAIL))
                .add(findResourceKey(Blocks.ACTIVATOR_RAIL))
                .add(findResourceKey(Blocks.DETECTOR_RAIL))
                .add(findResourceKey(Blocks.POWERED_RAIL))
                .add(findResourceKey(Blocks.NOTE_BLOCK))
                .add(findResourceKey(Blocks.OBSERVER))
                .add(findResourceKey(Blocks.HOPPER))
                .add(findResourceKey(Blocks.DROPPER))
                .add(findResourceKey(Blocks.DISPENSER))
                .add(findResourceKey(Blocks.HONEY_BLOCK))
                .add(findResourceKey(Blocks.SLIME_BLOCK))
                .add(findResourceKey(Blocks.PISTON))
                .add(findResourceKey(Blocks.STICKY_PISTON))
                .add(findResourceKey(Blocks.LIGHTNING_ROD))
                .add(findResourceKey(Blocks.DAYLIGHT_DETECTOR))
                .add(findResourceKey(Blocks.LECTERN))
                .add(findResourceKey(Blocks.TRIPWIRE_HOOK))
                .add(findResourceKey(Blocks.SCULK_SHRIEKER))
                .add(findResourceKey(Blocks.LEVER))
                .add(findResourceKey(Blocks.STONE_BUTTON))
                .add(findResourceKey(Blocks.OAK_PRESSURE_PLATE))
                .add(findResourceKey(Blocks.STONE_PRESSURE_PLATE))
                .add(findResourceKey(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE))
                .add(findResourceKey(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE))
                .add(findResourceKey(Blocks.SCULK_SENSOR))
                .add(findResourceKey(Blocks.CALIBRATED_SCULK_SENSOR))
                .add(findResourceKey(Blocks.REDSTONE_WIRE))
                .add(findResourceKey(Blocks.REDSTONE_TORCH))
                .add(findResourceKey(Blocks.REDSTONE_WALL_TORCH))
                .add(findResourceKey(Blocks.REDSTONE_BLOCK))
                .add(findResourceKey(Blocks.REPEATER))
                .add(findResourceKey(Blocks.COMPARATOR))
                .add(findResourceKey(Blocks.TARGET))
                .add(findResourceKey(Blocks.IRON_TRAPDOOR))
                .add(findResourceKey(Blocks.CAULDRON))
                .add(findResourceKey(Blocks.LAVA_CAULDRON))
                .add(findResourceKey(Blocks.WATER_CAULDRON))
                .add(findResourceKey(Blocks.POWDER_SNOW_CAULDRON))
                .add(findResourceKey(Blocks.CAMPFIRE))
                .add(findResourceKey(Blocks.ANVIL))
                .add(findResourceKey(Blocks.CHIPPED_ANVIL))
                .add(findResourceKey(Blocks.DAMAGED_ANVIL))
                .add(findResourceKey(ModBlocks.HEAVY_IRON_BLOCK.get()))
                .add(findResourceKey(ModBlocks.HEAVY_IRON_BEAM.get()))
                .add(findResourceKey(ModBlocks.HEAVY_IRON_COLUMN.get()))
                .add(findResourceKey(ModBlocks.HEAVY_IRON_PLATE.get()))
                .add(findResourceKey(ModBlocks.CUT_HEAVY_IRON_BLOCK.get()))
                .add(findResourceKey(ModBlocks.CUT_HEAVY_IRON_SLAB.get()))
                .add(findResourceKey(ModBlocks.CUT_HEAVY_IRON_STAIRS.get()))
                .add(findResourceKey(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get()))
                .add(findResourceKey(ModBlocks.POLISHED_HEAVY_IRON_SLAB.get()))
                .add(findResourceKey(ModBlocks.POLISHED_HEAVY_IRON_STAIRS.get()));

        provider.addTag(ModBlockTags.UNDER_CAULDRON)
                .addTag(BlockTags.CAMPFIRES)
                .add(findResourceKey(Blocks.MAGMA_BLOCK))
                .add(findResourceKey(ModBlocks.HEATER.get()))
                .add(findResourceKey(ModBlocks.CORRUPTED_BEACON.get()));
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
        provider.addTag(ModBlockTags.GLASS_BLOCKS).add(findResourceKey(ModBlocks.TEMPERING_GLASS.get()));
        provider.addTag(ModBlockTags.LASE_CAN_PASS_THROUGH)
                .addTag(ModBlockTags.GLASS_BLOCKS)
                .addTag(ModBlockTags.GLASS_PANES)
                .addTag(BlockTags.REPLACEABLE);
        provider.addTag(ModBlockTags.DEEPSLATE_METAL)
                .add(findResourceKey(Blocks.DEEPSLATE_GOLD_ORE))
                .add(findResourceKey(Blocks.DEEPSLATE_IRON_ORE))
                .add(findResourceKey(Blocks.DEEPSLATE_COPPER_ORE))
                .add(findResourceKey(ModBlocks.DEEPSLATE_ZINC_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TIN_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TITANIUM_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_LEAD_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_SILVER_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_URANIUM_ORE.get()));
        provider.addTag(ModBlockTags.ORES)
                .add(findResourceKey(ModBlocks.DEEPSLATE_ZINC_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TIN_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TITANIUM_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_LEAD_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_SILVER_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_URANIUM_ORE.get()))
                .add(findResourceKey(ModBlocks.VOID_STONE.get()))
                .add(findResourceKey(ModBlocks.EARTH_CORE_SHARD_ORE.get()));
        provider.addTag(ModBlockTags.ORES_IN_GROUND_DEEPSLATE)
                .add(findResourceKey(ModBlocks.DEEPSLATE_ZINC_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TIN_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TITANIUM_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_LEAD_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_SILVER_ORE.get()))
                .add(findResourceKey(ModBlocks.DEEPSLATE_URANIUM_ORE.get()))
                .add(findResourceKey(ModBlocks.VOID_STONE.get()))
                .add(findResourceKey(ModBlocks.EARTH_CORE_SHARD_ORE.get()));
        provider.addTag(ModBlockTags.END_PORTAL_UNABLE_CHANGE).add(findResourceKey(Blocks.DRAGON_EGG));
    }
}
