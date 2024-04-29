package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PowerConverterSmallBlock extends BasePowerConverterBlock {
    public static final VoxelShape SHAPE_DOWN = Block.box(7, 0, 7, 9, 8, 9);
    public static final VoxelShape SHAPE_UP = Block.box(7, 8, 7, 9, 16, 9);
    public static final VoxelShape SHAPE_NORTH = Block.box(7, 7, 0, 9, 9, 8);
    public static final VoxelShape SHAPE_EASE = Block.box(8, 7, 7, 16, 9, 9);
    public static final VoxelShape SHAPE_SOUTH = Block.box(7, 7, 8, 9, 9, 16);
    public static final VoxelShape SHAPE_WEST = Block.box(0, 7, 7, 8, 9, 9);

    public PowerConverterSmallBlock(Properties properties) {
        super(properties, 1);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EASE;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
        };
    }
}
