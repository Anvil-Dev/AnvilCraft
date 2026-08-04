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

public class PowerConverterSuperBigBlock extends BasePowerConverterBlock implements IHammerRemovable {
    public static final VoxelShape SHAPE_DOWN = Block.box(4, 0, 4, 12, 8, 12);
    public static final VoxelShape SHAPE_UP = Block.box(4, 8, 4, 12, 16, 12);
    public static final VoxelShape SHAPE_NORTH = Block.box(4, 4, 0, 12, 12, 8);
    public static final VoxelShape SHAPE_EAST = Block.box(8, 4, 4, 16, 12, 12);
    public static final VoxelShape SHAPE_SOUTH = Block.box(4, 4, 8, 12, 12, 16);
    public static final VoxelShape SHAPE_WEST = Block.box(0, 4, 4, 8, 12, 12);

    public static final int INPUT_TIME = 4096;

    public PowerConverterSuperBigBlock(Properties properties) {
        super(properties, INPUT_TIME);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(PowerConverterSuperBigBlock::new);
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
