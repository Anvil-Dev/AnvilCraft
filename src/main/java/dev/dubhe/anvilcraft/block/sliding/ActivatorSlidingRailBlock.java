package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.entity.ActivatorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ActivatorSlidingRailBlock extends BaseSlidingRailBlock implements IHammerChangeable, IMoveableEntityBlock {
    public static final List<Direction> SIGNAL_SOURCE_SIDES = List.of(
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static final VoxelShape AABB_X = Stream.of(
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(0, 6, 11, 16, 16, 16),
        Block.box(0, 6, 0, 16, 16, 5)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_Z =
        Stream.of(
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(11, 6, 0, 16, 16, 16),
            Block.box(0, 6, 0, 5, 16, 16)
        ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ActivatorSlidingRailBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(POWERED, this.isPowered(context.getLevel(), context.getClickedPos(), facing));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    protected boolean findActivatorSlidingRailSignal(Level level, BlockPos pos, Direction facing, boolean searchForward) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        switch (facing.getAxis()) {
            case X -> x += searchForward ? 1 : -1;
            case Z -> z += searchForward ? 1 : -1;
        }

        return this.isSameRailWithPower(level, new BlockPos(x, y, z), searchForward, 0, facing);
    }

    protected boolean findActivatorSlidingRailSignal(
        Level level, BlockPos pos, BlockState state, boolean searchForward, int recursionCount
    ) {
        if (recursionCount >= 8) return false;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!state.hasProperty(FACING)) return false;
        Direction facing = state.getValue(FACING);
        switch (facing.getAxis()) {
            case X -> x += searchForward ? 1 : -1;
            case Z -> z += searchForward ? 1 : -1;
        }

        return this.isSameRailWithPower(level, new BlockPos(x, y, z), searchForward, recursionCount, facing);
    }

    protected boolean isSameRailWithPower(Level level, BlockPos pos, boolean searchForward, int recursionCount, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ActivatorSlidingRailBlock other)) return false;
        Direction otherFacing = state.getValue(FACING);
        if (facing.getAxis() != otherFacing.getAxis()) return false;
        return level.hasNeighborSignal(pos)
               || other.findActivatorSlidingRailSignal(level, pos, state, searchForward, recursionCount + 1);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> AABB_X;
            case Z -> AABB_Z;
            default -> super.getShape(state, level, pos, ctx);
        };
    }

    private static final int[] UPDATE_POS = new int[] {-1, 1};

    protected void updatePower(Level level, BlockPos pos, BlockState state, BlockPos fromPos) {
        boolean powered = state.getValue(POWERED);
        boolean shouldPower = this.isPowered(level, pos);
        if (powered != shouldPower) {
            level.setBlockAndUpdate(pos, state.setValue(POWERED, shouldPower));
        }
        if (powered) {
            Direction.Axis axis = state.getValue(FACING).getAxis();
            for (int updatePos : UPDATE_POS) {
                BlockPos pos1 = pos.relative(axis, updatePos);
                if (pos1.equals(fromPos)) continue;
                BlockState state1 = level.getBlockState(pos1);
                if (!(state1.getBlock() instanceof ActivatorSlidingRailBlock other)) continue;
                if (state1.getOptionalValue(FACING).map(Direction::getAxis).filter(axis::equals).isEmpty()) continue;
                level.neighborChanged(pos1, other, pos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        this.updatePower(level, pos, state, fromPos);
        Optional<ActivatorSlidingRailBlockEntity> beOp = level.getBlockEntity(pos, ModBlockEntities.ACTIVATOR_SLIDING_RAIL.get());
        if (fromPos.equals(pos.above())
            && state.getValue(POWERED)
            && !beOp.map(ActivatorSlidingRailBlockEntity::isShouldPower).orElse(false)
            && !level.getBlockTicks().hasScheduledTick(pos, this)
            && !MOVING_PISTON_MAP.containsKey(fromPos)
            && block.equals(Blocks.MOVING_PISTON)
        ) {
            beOp.ifPresent(ActivatorSlidingRailBlockEntity::shouldPower);
            level.scheduleTick(pos, this, 3);
            this.updateAbove(level, pos);
            return;
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Optional<ActivatorSlidingRailBlockEntity> beOp = level.getBlockEntity(pos, ModBlockEntities.ACTIVATOR_SLIDING_RAIL.get());
        if (beOp.map(ActivatorSlidingRailBlockEntity::isShouldPower).orElse(false)) {
            beOp.ifPresent(ActivatorSlidingRailBlockEntity::shouldNotPower);
            level.scheduleTick(pos, this, 5);
            this.updateAbove(level, pos);
            return;
        } else if (state.getValue(POWERED)) {
            BlockPos fromPos = pos.above();
            if (level.isEmptyBlock(fromPos)) return;
            PistonPushInfo ppi = new PistonPushInfo(fromPos, state.getValue(FACING));
            ppi.extending = true;
            MOVING_PISTON_MAP.put(pos, ppi);
        }
        super.tick(state, level, pos, random);
    }

    private boolean isPowered(Level level, BlockPos pos, Direction facing) {
        for (Direction side : SIGNAL_SOURCE_SIDES) {
            if (level.getSignal(pos.relative(side), side) > 0) return true;
        }
        return this.findActivatorSlidingRailSignal(level, pos, facing, true)
               || this.findActivatorSlidingRailSignal(level, pos, facing, false);
    }

    private boolean isPowered(Level level, BlockPos pos) {
        for (Direction side : SIGNAL_SOURCE_SIDES) {
            if (level.getSignal(pos.relative(side), side) > 0) return true;
        }
        BlockState state = level.getBlockState(pos);
        return this.findActivatorSlidingRailSignal(level, pos, state, true, 0)
               || this.findActivatorSlidingRailSignal(level, pos, state, false, 0);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(POWERED)) return 0;
        if (!level.getBlockEntity(pos, ModBlockEntities.ACTIVATOR_SLIDING_RAIL.get())
            .map(ActivatorSlidingRailBlockEntity::isShouldPower)
            .orElse(false)
        ) return 0;
        return direction == Direction.DOWN ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.getSignal(state, level, pos, direction);
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        BlockState bs = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, bs.cycle(FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity) {
        if (entity.getStartPos().equals(pos.above())) return;
        if (!state.getValue(POWERED)) return;
        level.setBlockAndUpdate(pos, state.setValue(FACING, entity.getMoveDirection()));
        level.getBlockEntity(pos, ModBlockEntities.ACTIVATOR_SLIDING_RAIL.get()).ifPresent(ActivatorSlidingRailBlockEntity::shouldPower);
        ISlidingRail.stopSlidingBlock(entity);
        level.scheduleTick(pos, this, 3);
    }

    private void updateAbove(Level level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        aboveState.onNeighborChange(level, abovePos, pos);
        level.neighborChanged(aboveState, abovePos, this, pos, false);
        if (!aboveState.isRedstoneConductor(level, abovePos)) return;
        abovePos = abovePos.above();
        aboveState = level.getBlockState(abovePos);
        if (!aboveState.getWeakChanges(level, abovePos)) return;
        level.neighborChanged(aboveState, abovePos, this, pos, false);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.ACTIVATOR_SLIDING_RAIL.create(pos, state);
    }
}
