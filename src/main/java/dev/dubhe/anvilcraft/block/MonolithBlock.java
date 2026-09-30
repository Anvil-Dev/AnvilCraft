package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

/**
 * 普通石碑方块，采用类似原版蘑菇块的六面独立纹理机制。
 * 与任意石碑类方块（核心、竖线等）相邻的面显示 monolith_inner.png，
 * 否则显示 monolith.png。
 */
public class MonolithBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
        Direction.NORTH, NORTH,
        Direction.EAST, EAST,
        Direction.SOUTH, SOUTH,
        Direction.WEST, WEST,
        Direction.UP, UP,
        Direction.DOWN, DOWN
    );

    public MonolithBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(NORTH, true)
                .setValue(EAST, true)
                .setValue(SOUTH, true)
                .setValue(WEST, true)
                .setValue(UP, true)
                .setValue(DOWN, true)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState()
            .setValue(DOWN, !level.getBlockState(pos.below()).is(ModBlockTags.MONOLITH_BLOCKS))
            .setValue(UP, !level.getBlockState(pos.above()).is(ModBlockTags.MONOLITH_BLOCKS))
            .setValue(NORTH, !level.getBlockState(pos.north()).is(ModBlockTags.MONOLITH_BLOCKS))
            .setValue(EAST, !level.getBlockState(pos.east()).is(ModBlockTags.MONOLITH_BLOCKS))
            .setValue(SOUTH, !level.getBlockState(pos.south()).is(ModBlockTags.MONOLITH_BLOCKS))
            .setValue(WEST, !level.getBlockState(pos.west()).is(ModBlockTags.MONOLITH_BLOCKS));
    }

    @Override
    protected BlockState updateShape(
        BlockState state, Direction facing, BlockState facingState,
        LevelAccessor level, BlockPos currentPos, BlockPos facingPos
    ) {
        return facingState.is(ModBlockTags.MONOLITH_BLOCKS)
            ? state.setValue(PROPERTY_BY_DIRECTION.get(facing), false)
            : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            result = result.setValue(
                PROPERTY_BY_DIRECTION.get(rotation.rotate(direction)),
                state.getValue(PROPERTY_BY_DIRECTION.get(direction))
            );
        }
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            result = result.setValue(
                PROPERTY_BY_DIRECTION.get(mirror.mirror(direction)),
                state.getValue(PROPERTY_BY_DIRECTION.get(direction))
            );
        }
        return result;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }
}
