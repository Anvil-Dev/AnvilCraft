package dev.dubhe.anvilcraft.block.cfa.interfaces;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.util.ShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CelestialForgingAnvilLogisticsInterfaceBlock extends CelestialForgingAnvilInterfaceBlock {
    public static final VoxelShape NORTH = ShapeUtil.merge(
        CelestialForgingAnvilInterfaceBlock.BASE_NORTH,
        Block.box(4, 4, 0, 12, 12, 8),
        Block.box(4, 8, 6, 12, 16, 14),
        Block.box(5, 12, 1, 11, 18, 7)
    );
    public static final VoxelShape WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, NORTH);
    public static final VoxelShape SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, NORTH);
    public static final VoxelShape EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, NORTH);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(CelestialForgingAnvilLogisticsInterfaceBlock::new);
    }

    public CelestialForgingAnvilLogisticsInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> throw new IllegalArgumentException("Unsupported direction for horizontal facing");
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
