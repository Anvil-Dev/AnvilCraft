package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FeCollectorBlock extends BetterBaseEntityBlock implements HammerRotateBehavior, IHammerRemovable {
    private static final VoxelShape SHAPE_X = ShapeUtil.merge(
        Block.box(0, 0, 0, 16, 4, 16),
        Block.box(0, 4, 4, 2, 12, 12),
        Block.box(14, 4, 4, 16, 12, 12)
    );
    private static final VoxelShape SHAPE_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, SHAPE_X);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FeCollectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.getStateDefinition()
                .any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(POWERED, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FeCollectorBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS).add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getHorizontalDirection();
        Direction.Axis axis = switch (dir) {
            case NORTH, SOUTH -> Direction.Axis.X;
            case WEST, EAST -> Direction.Axis.Z;
            default -> Direction.Axis.X;
        };
        return this.defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeCollectorBlockEntity(pos, state);
    }

    public void activate(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(POWERED, true));
        this.updateNeighbours(level, pos);
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) return;
        level.setBlockAndUpdate(pos, state.setValue(POWERED, false));
        this.updateNeighbours(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide() || state.is(oldState.getBlock())) return;
        if (state.getValue(POWERED) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.setBlock(pos, state.setValue(POWERED, false), 18);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && state.getValue(POWERED)) {
            this.updateNeighbours(level, pos);
        }
    }

    private void updateNeighbours(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.below(), this);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
            type,
            ModBlockEntities.FE_COLLECTOR.get(),
            FeCollectorBlockEntity::tick
        );
    }
}
