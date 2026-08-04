package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PowerConverterExtremelyBigBlock extends BasePowerConverterBlock implements IHammerRemovable {
    public static final VoxelShape SHAPE_DOWN = Block.box(3, 0, 3, 13, 16, 13);
    public static final VoxelShape SHAPE_UP = Block.box(3, 0, 3, 13, 16, 13);
    public static final VoxelShape SHAPE_NORTH = Block.box(3, 3, 0, 13, 13, 16);
    public static final VoxelShape SHAPE_EAST = Block.box(0, 3, 3, 16, 13, 13);
    public static final VoxelShape SHAPE_SOUTH = Block.box(3, 3, 0, 13, 13, 16);
    public static final VoxelShape SHAPE_WEST = Block.box(0, 3, 3, 16, 13, 13);

    public static final int INPUT_TIME = 65536;

    public PowerConverterExtremelyBigBlock(Properties properties) {
        super(properties, INPUT_TIME);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(PowerConverterExtremelyBigBlock::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
        };
    }
}
