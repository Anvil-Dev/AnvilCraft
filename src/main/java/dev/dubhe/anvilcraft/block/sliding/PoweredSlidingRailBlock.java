package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PoweredSlidingRailBlock extends BaseSlidingRailBlock implements IHammerChangeable {
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

    public PoweredSlidingRailBlock(Properties properties) {
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

    protected boolean findPoweredSlidingRailSignal(Level level, BlockPos pos, Direction facing, boolean searchForward) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        switch (facing) {
            case NORTH -> z -= searchForward ? 1 : -1;
            case SOUTH -> z += searchForward ? 1 : -1;
            case EAST -> x += searchForward ? 1 : -1;
            case WEST -> x -= searchForward ? 1 : -1;
        }

        return this.isSameRailWithPower(level, new BlockPos(x, y, z), searchForward, 0, facing);
    }

    protected boolean findPoweredSlidingRailSignal(Level level, BlockPos pos, BlockState state, boolean searchForward, int recursionCount) {
        if (recursionCount >= 8) return false;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!state.hasProperty(FACING)) return false;
        Direction facing = state.getValue(FACING);
        switch (facing) {
            case NORTH -> z -= searchForward ? 1 : -1;
            case SOUTH -> z += searchForward ? 1 : -1;
            case EAST -> x += searchForward ? 1 : -1;
            case WEST -> x -= searchForward ? 1 : -1;
        }

        return this.isSameRailWithPower(level, new BlockPos(x, y, z), searchForward, recursionCount, facing);
    }

    protected boolean isSameRailWithPower(Level level, BlockPos pos, boolean searchForward, int recursionCount, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PoweredSlidingRailBlock other)) return false;
        Direction otherFacing = state.getValue(FACING);
        if (facing != otherFacing) return false;
        return level.hasNeighborSignal(pos)
               || other.findPoweredSlidingRailSignal(level, pos, state, searchForward, recursionCount + 1);
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

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        if (!state.getValue(POWERED)) return;
        super.onNeighborChange(state, level, pos, neighbor);
    }

    private static final int[] UPDATE_POS = new int[] {-1, 1};

    protected boolean updatePower(Level level, BlockPos pos, BlockState state, BlockPos fromPos) {
        boolean powered = state.getValue(POWERED);
        boolean shouldPower = this.isPowered(level, pos);
        if (powered != shouldPower) {
            powered = shouldPower;
            level.setBlockAndUpdate(pos, state.setValue(POWERED, shouldPower));
        }
        if (powered) {
            Direction.Axis axis = state.getValue(FACING).getAxis();
            for (int updatePos : UPDATE_POS) {
                BlockPos pos1 = pos.relative(axis, updatePos);
                if (pos1.equals(fromPos)) continue;
                BlockState state1 = level.getBlockState(pos1);
                if (!(state1.getBlock() instanceof PoweredSlidingRailBlock other)) continue;
                if (state1.getOptionalValue(FACING).map(Direction::getAxis).filter(axis::equals).isEmpty()) continue;
                level.neighborChanged(pos1, other, pos);
            }
        }
        return powered;
    }

    @Override
    public boolean getWeakChanges(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean powered = this.updatePower(level, pos, state, fromPos);
        if (powered) {
            fromPos = pos.above();
//            if (level.isEmptyBlock(fromPos)) {
//                BlockPos stop = pos.relative(state.getValue(FACING).getOpposite());
//                if (!level.getBlockState(stop).is(ModBlocks.SLIDING_RAIL_STOP) || level.isEmptyBlock(stop.above())) return;
//                BlockState above = level.getBlockState(stop.above());
//                level.setBlock(stop.above(), Blocks.AIR.defaultBlockState(), 0b1000011);
//                level.setBlock(fromPos, above, 0b1000011);
//            }
            PistonPushInfo ppi = new PistonPushInfo(fromPos, state.getValue(FACING));
            ppi.extending = true;
            if (MOVING_PISTON_MAP.containsKey(pos)) {
                MOVING_PISTON_MAP.get(pos).fromPos = fromPos;
            } else MOVING_PISTON_MAP.put(pos, ppi);
        }
        if (level.isClientSide) return;
        BlockState blockState = level.getBlockState(MOVING_PISTON_MAP.get(pos) instanceof PistonPushInfo info ? info.fromPos : fromPos);
        if (!MOVING_PISTON_MAP.containsKey(pos)) return;
        if (blockState.is(Blocks.MOVING_PISTON) || blockState.isAir()) return;
        level.scheduleTick(pos, this, 2);
    }

    private boolean isPowered(Level level, BlockPos pos, Direction facing) {
        for (Direction side : SIGNAL_SOURCE_SIDES) {
            if (level.getSignal(pos.relative(side), side) > 0) return true;
        }
        return this.findPoweredSlidingRailSignal(level, pos, facing, true)
               || this.findPoweredSlidingRailSignal(level, pos, facing, false);
    }

    private boolean isPowered(Level level, BlockPos pos) {
        for (Direction side : SIGNAL_SOURCE_SIDES) {
            if (level.getSignal(pos.relative(side), side) > 0) return true;
        }
        BlockState state = level.getBlockState(pos);
        return this.findPoweredSlidingRailSignal(level, pos, state, true, 0)
               || this.findPoweredSlidingRailSignal(level, pos, state, false, 0);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.getType() != EntityType.ITEM) return;
        if (!state.getValue(POWERED)) {
            ISlidingRail.absorbEntity(pos, entity);
        } else {
            entity.setDeltaMovement(Vec3.ZERO.relative(state.getValue(FACING), 0.5));
        }
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        BlockState bs = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, bs.cycle(FACING));
        return true;
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity) {
        if (!state.getValue(POWERED)) {
            ISlidingRail.stopSlidingBlock(entity);
            return;
        }
        Vec3 entityPos = entity.position();
        Direction facing = state.getValue(FACING);
        Direction.Axis horizontalAnother = facing.getClockWise().getAxis();
        double single = entityPos.get(horizontalAnother);
        double should = Math.ceil(single) - 0.5;
        if (Math.abs(should - single) > 0.25) return;
        entity.setMoveDirection(facing);
    }

    @Override
    public boolean canMoveBlockToTop(LevelReader level, BlockPos pos, BlockState state, BlockState top, Direction side) {
        return state.getValue(POWERED) && state.getValue(FACING) == side.getOpposite();
    }
}
