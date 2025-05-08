package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerConverterBigBlock extends BasePowerConverterBlock implements IHammerRemovable {
    public static final VoxelShape SHAPE_DOWN = Block.box(5, 0, 5, 11, 8, 11);
    public static final VoxelShape SHAPE_UP = Block.box(5, 8, 5, 11, 16, 11);
    public static final VoxelShape SHAPE_NORTH = Block.box(5, 5, 0, 11, 11, 8);
    public static final VoxelShape SHAPE_EASE = Block.box(8, 5, 5, 16, 11, 11);
    public static final VoxelShape SHAPE_SOUTH = Block.box(5, 5, 8, 11, 11, 16);
    public static final VoxelShape SHAPE_WEST = Block.box(0, 5, 5, 8, 11, 11);

    public PowerConverterBigBlock(Properties properties) {
        super(properties, 36);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(PowerConverterBigBlock::new);
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
