package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class MultiPartBlockUtil {
    public static BlockPos getMainPartPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof AbstractMultiPartBlock<?> multiplePartBlock) {
            BlockPos mainPartPos = multiplePartBlock.getMainPartPos(pos, state);
            BlockState mainPartState = level.getBlockState(mainPartPos);
            if (mainPartState.is(block)) pos = mainPartPos;
        } else if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            BlockPos mainPartPos = pos.below();
            if (level.getBlockState(mainPartPos).is(block)) pos = mainPartPos;
        } else if (state.hasProperty(BlockStateProperties.BED_PART)
            && state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT) {
            BlockPos mainPartPos = pos.relative(state.getValue(HORIZONTAL_FACING));
            if (level.getBlockState(mainPartPos).is(block)) pos = mainPartPos;
        } else if (state.is(Blocks.PISTON_HEAD)) {
            BlockPos mainPartPos = pos.relative(state.getValue(FACING).getOpposite());
            BlockState mainPartState = level.getBlockState(mainPartPos);
            if (mainPartState.is(Blocks.PISTON)) pos = mainPartPos;
            if (mainPartState.is(Blocks.STICKY_PISTON)) pos = mainPartPos;
        }
        return pos;
    }

    /**
     * 获取主方块位置，只包含可“连锁破坏”的多方块
     */
    public static BlockPos getChainableMainPartPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.LARGE_CAKE)) return pos;
        return getMainPartPos(level, pos);
    }
}
