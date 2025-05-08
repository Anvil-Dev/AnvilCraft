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
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerConverterMiddleBlock extends BasePowerConverterBlock implements IHammerRemovable {
    public static final VoxelShape SHAPE_DOWN = Block.box(6, 0, 6, 10, 8, 10);
    public static final VoxelShape SHAPE_UP = Block.box(6, 8, 6, 10, 16, 10);
    public static final VoxelShape SHAPE_NORTH = Block.box(6, 6, 0, 10, 10, 8);
    public static final VoxelShape SHAPE_EASE = Block.box(8, 6, 6, 16, 10, 10);
    public static final VoxelShape SHAPE_SOUTH = Block.box(6, 6, 8, 10, 10, 16);
    public static final VoxelShape SHAPE_WEST = Block.box(0, 6, 6, 8, 10, 10);

    public PowerConverterMiddleBlock(Properties properties) {
        super(properties, 6);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(PowerConverterMiddleBlock::new);
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
