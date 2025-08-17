package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockComparatorBlock extends HorizontalDirectionalBlock implements HammerRotateBehavior, IHammerRemovable {

    public static final MapCodec<BlockComparatorBlock> CODEC = simpleCodec(BlockComparatorBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PRECISE = BooleanProperty.create("precise");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final VoxelShape NORTH_MODEL = Shapes.or(Block.box(0, 4, 0, 16, 7, 6), Block.box(4, 0, 3, 12, 8, 16));
    public static final VoxelShape EAST_MODEL = Shapes.or(Block.box(10, 4, 0, 16, 7, 16), Block.box(0, 0, 4, 13, 8, 12));
    public static final VoxelShape SOUTH_MODEL = Shapes.or(Block.box(0, 4, 10, 16, 7, 16), Block.box(4, 0, 0, 12, 8, 13));
    public static final VoxelShape WEST_MODEL = Shapes.or(Block.box(0, 4, 0, 6, 7, 16), Block.box(3, 0, 4, 16, 8, 12));

    public BlockComparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PRECISE, false)
                .setValue(POWERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(PRECISE).add(POWERED);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
            case NORTH -> NORTH_MODEL;
            case SOUTH -> SOUTH_MODEL;
            case EAST -> EAST_MODEL;
            case WEST -> WEST_MODEL;
            default -> super.getShape(state, level, pos, context);
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        return this.defaultBlockState().setValue(FACING, direction.getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || (oldState.is(this) && state.getValue(FACING) == oldState.getValue(FACING)))
            return;
        boolean newPowered = checkBlocks(level, pos, state);
        level.setBlock(pos, state.setValue(POWERED, newPowered), 3);
        this.updateNeighborsInFront(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level.isClientSide || (state.is(newState.getBlock()) && state.getValue(FACING) == newState.getValue(FACING)))
            return;
        if (state.getValue(POWERED)) {
            this.updateNeighborsInFront(level, pos, state);
        }

    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            BlockState newState = state.cycle(PRECISE);
            level.setBlock(pos, newState.setValue(POWERED, checkBlocks(level, pos, newState)), 2);
            this.updateNeighborsInFront(level, pos, state);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    private boolean checkBlocks(LevelAccessor level, BlockPos pos, BlockState blockState) {
        Direction facing = blockState.getValue(FACING);
        BlockState state1 = level.getBlockState(pos.relative(facing.getClockWise()));
        BlockState state2 = level.getBlockState(pos.relative(facing.getCounterClockWise()));
        return blockState.getValue(PRECISE) ? state1.equals(state2) : state1.getBlock() == state2.getBlock();
    }

    @Override
    public BlockState updateShape(
        BlockState blockState,
        Direction direction,
        BlockState blockState2,
        LevelAccessor level,
        BlockPos pos,
        BlockPos pos2
    ) {
        Direction facing = blockState.getValue(FACING);
        if (direction.getAxis() == Direction.Axis.Y || direction.getAxis() == facing.getAxis()) return blockState;
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2);
        }
        return blockState;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean same = checkBlocks(level, pos, state);
        if (same != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, same), 2);
            this.updateNeighborsInFront(level, pos, state);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction.getOpposite());
        level.neighborChanged(blockpos, this, pos);
        level.updateNeighborsAtExceptFromFacing(blockpos, this, direction);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction == state.getValue(FACING);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getSignal(blockAccess, pos, side);
    }

    @Override
    protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(POWERED) && blockState.getValue(FACING) == side ? 15 : 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
