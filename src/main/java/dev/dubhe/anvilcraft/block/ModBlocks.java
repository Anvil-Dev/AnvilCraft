package dev.dubhe.anvilcraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ModBlocks {
    public static final Map<String, Block> BLOCK_MAP = new HashMap<>();

    public static final Block MAGNET_BLOCK = registerBlock("magnet_block", Block::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block HOLLOW_MAGNET_BLOCK = registerBlock("hollow_magnet_block", HollowMagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
    public static final Block FERRITE_CORE_MAGNET_BLOCK = registerBlock("ferrite_core_magnet_block", FerriteCoreMagnetBlock::new, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));


    private static Block registerBlock(String id, @NotNull Function<BlockBehaviour.Properties, Block> blockCreator, BlockBehaviour.Properties properties) {
        Block block = blockCreator.apply(properties);
        BLOCK_MAP.put(id, block);
        return block;
    }
}
