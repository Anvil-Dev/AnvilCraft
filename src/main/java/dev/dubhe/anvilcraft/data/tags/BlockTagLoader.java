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
            .add(findResourceKey(Blocks.LIGHTNING_ROD));

        provider.addTag(ModBlockTags.HAMMER_REMOVABLE)
            .add(findResourceKey(Blocks.BELL))
            .add(findResourceKey(Blocks.REDSTONE_LAMP))
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
            .add(findResourceKey(Blocks.SCULK_SENSOR))
            .add(findResourceKey(Blocks.CALIBRATED_SCULK_SENSOR))
            .add(findResourceKey(Blocks.REDSTONE_WIRE))
            .add(findResourceKey(Blocks.REDSTONE_TORCH))
            .add(findResourceKey(Blocks.REDSTONE_WALL_TORCH))
            .add(findResourceKey(Blocks.REDSTONE_BLOCK))
            .add(findResourceKey(Blocks.REPEATER))
            .add(findResourceKey(Blocks.COMPARATOR))
            .add(findResourceKey(Blocks.TARGET))
            .add(ModBlocks.HEAVY_IRON_BLOCK.getKey())
            .add(ModBlocks.HEAVY_IRON_BEAM.getKey())
            .add(ModBlocks.HEAVY_IRON_COLUMN.getKey())
            .add(ModBlocks.HEAVY_IRON_PLATE.getKey())
            .add(ModBlocks.CUT_HEAVY_IRON_BLOCK.getKey())
            .add(ModBlocks.CUT_HEAVY_IRON_SLAB.getKey())
            .add(ModBlocks.CUT_HEAVY_IRON_STAIRS.getKey())
            .add(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.getKey())
            .add(ModBlocks.POLISHED_HEAVY_IRON_SLAB.getKey())
            .add(ModBlocks.POLISHED_HEAVY_IRON_STAIRS.getKey())
            .addTag(BlockTags.BUTTONS)
            .addTag(BlockTags.DOORS)
            .addTag(BlockTags.PRESSURE_PLATES)
            .addTag(BlockTags.TRAPDOORS)
            .addTag(BlockTags.ANVIL)
            .addTag(BlockTags.RAILS)
            .addTag(BlockTags.CAULDRONS)
            .addTag(BlockTags.CAMPFIRES);

        provider.addTag(ModBlockTags.UNDER_CAULDRON)
            .addTag(BlockTags.CAMPFIRES)
            .add(findResourceKey(Blocks.MAGMA_BLOCK))
            .add(ModBlocks.HEATER.getKey())
            .add(ModBlocks.CORRUPTED_BEACON.getKey());

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

        provider.addTag(ModBlockTags.INCORRECT_FOR_AMETHYST_TOOL)
            .addTag(BlockTags.INCORRECT_FOR_STONE_TOOL);

        provider.addTag(ModBlockTags.INCORRECT_FOR_EMBER_TOOL);

        provider.addTag(ModBlockTags.END_PORTAL_UNABLE_CHANGE).add(findResourceKey(Blocks.DRAGON_EGG));

        provider.addTag(ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)
            .add(findResourceKey(Blocks.END_STONE))
            .add(findResourceKey(Blocks.BEDROCK))
            .add(ModBlocks.END_DUST.getKey())
            .add(ModBlocks.NEGATIVE_MATTER_BLOCK.getKey());

        provider.addTag(ModBlockTags.VOID_DECAY_PRODUCTS)
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
            .add(ModBlocks.VOID_STONE.getKey())
            .add(ModBlocks.END_DUST.getKey())
            .add(ModBlocks.CURSED_GOLD_BLOCK.getKey())
            .add(ModBlocks.DEEPSLATE_TIN_ORE.getKey())
            .add(ModBlocks.DEEPSLATE_ZINC_ORE.getKey())
            .add(ModBlocks.DEEPSLATE_LEAD_ORE.getKey())
            .add(ModBlocks.DEEPSLATE_URANIUM_ORE.getKey());

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
    }
}
