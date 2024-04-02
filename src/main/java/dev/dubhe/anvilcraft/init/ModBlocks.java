package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ModBlocks {
    private static final Map<String, Block> BLOCK_MAP = new HashMap<>();

    public static final Block STAMPING_PLATFORM = registerBlock("stamping_platform", StampingPlatformBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block ROYAL_ANVIL = registerBlock("royal_anvil", RoyalAnvilBlock::new, BlockBehaviour.Properties.copy(Blocks.ANVIL));
    public static final Block MAGNET_BLOCK = registerBlock("magnet_block", MagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block HOLLOW_MAGNET_BLOCK = registerBlock("hollow_magnet_block", HollowMagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block FERRITE_CORE_MAGNET_BLOCK = registerBlock("ferrite_core_magnet_block", FerriteCoreMagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).randomTicks());
    public static final Block CHUTE = registerBlock("chute", ChuteBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion());
    public static final Block AUTO_CRAFTER = registerBlock("auto_crafter", AutoCrafterBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block ROYAL_GRINDSTONE = registerBlock("royal_grindstone", RoyalGrindstone::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block ROYAL_SMITHING_TABLE = registerBlock("royal_smithing_table", RoyalSmithingTableBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block ROYAL_STEEL_BLOCK = registerBlock("royal_steel_block", Block::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block SMOOTH_ROYAL_STEEL_BLOCK = registerBlock("smooth_royal_steel_block", Block::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block CUT_ROYAL_STEEL_BLOCK = registerBlock("cut_royal_steel_block", Block::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block CUT_ROYAL_STEEL_SLAB = registerBlock("cut_royal_steel_slab", SlabBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block CUT_ROYAL_STEEL_STAIRS = registerBlock("cut_royal_steel_stairs", (properties) -> new StairBlock(ModBlocks.CUT_ROYAL_STEEL_BLOCK.defaultBlockState(), properties), BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block LAVA_CAULDRON = registerBlock("lava_cauldron", LavaCauldronBlock::new, BlockBehaviour.Properties.copy(Blocks.LAVA_CAULDRON).lightLevel(blockState -> blockState.getValue(LayeredCauldronBlock.LEVEL) * 5));
    public static final Block CURSED_GOLD_BLOCK = registerBlock("cursed_gold_block", Block::new, BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK));

    public static void register() {
        for (Map.Entry<String, Block> entry : ModBlocks.BLOCK_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.BLOCK, AnvilCraft.of(entry.getKey()), entry.getValue());
        }
    }

    private static Block registerBlock(String id, @NotNull Function<BlockBehaviour.Properties, Block> blockCreator, BlockBehaviour.Properties properties) {
        Block block = blockCreator.apply(properties);
        BLOCK_MAP.put(id, block);
        return block;
    }
}
