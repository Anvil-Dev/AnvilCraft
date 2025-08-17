package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SlidingRailBlock extends BaseSlidingRailBlock implements IHammerChangeable {
    public static final VoxelShape AABB_X = Stream.of(
        Block.box(0, 6, 11, 16, 12, 14),
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(0, 12, 0, 16, 16, 5),
        Block.box(0, 12, 11, 16, 16, 16),
        Block.box(0, 6, 2, 16, 12, 5)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_Z = Stream.of(
        Block.box(2, 6, 0, 5, 12, 16),
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(11, 12, 0, 16, 16, 16),
        Block.box(0, 12, 0, 5, 16, 16),
        Block.box(11, 6, 0, 14, 12, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_Y = Stream.of(
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(11, 6, 11, 16, 16, 16),
        Block.box(0, 6, 11, 5, 16, 16),
        Block.box(0, 6, 0, 5, 16, 5),
        Block.box(11, 6, 0, 16, 16, 5)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

    public SlidingRailBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(AXIS, Axis.X));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Axis axis = context.getHorizontalDirection().getOpposite().getAxis();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if ((isOtherRailInAxis(level, pos, Axis.X, -1) == TriState.TRUE
             || isOtherRailInAxis(level, pos, Axis.X, 1) == TriState.TRUE)
            && (isOtherRailInAxis(level, pos, Axis.Z, -1) == TriState.TRUE
                || isOtherRailInAxis(level, pos, Axis.Z, 1) == TriState.TRUE)
        ) axis = Axis.Y;
        return this.defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
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
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext
    ) {
        return switch (blockState.getValue(AXIS)) {
            case X -> AABB_X;
            case Y -> AABB_Y;
            case Z -> AABB_Z;
        };
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (isOtherRailInAxis(level, pos, Axis.X, -1) == TriState.TRUE
            || isOtherRailInAxis(level, pos, Axis.X, 1) == TriState.TRUE
        ) {
            if (state.getValue(AXIS) != Axis.Y
                && (isOtherRailInAxis(level, pos, Axis.Z, -1) == TriState.TRUE
                    || isOtherRailInAxis(level, pos, Axis.Z, 1) == TriState.TRUE)
            ) {
                level.setBlockAndUpdate(pos, state.setValue(AXIS, Axis.Y));
            }
            if (state.getValue(AXIS) == Axis.Y
                && isOtherRailInAxis(level, pos, Axis.Z, -1) != TriState.TRUE
                && isOtherRailInAxis(level, pos, Axis.Z, 1) != TriState.TRUE
            ) {
                level.setBlockAndUpdate(pos, state.setValue(AXIS, Axis.X));
            }
        } else if (
            isOtherRailInAxis(level, pos, Axis.Z, -1) == TriState.TRUE
            || isOtherRailInAxis(level, pos, Axis.Z, 1) == TriState.TRUE
        ) {
            if (state.getValue(AXIS) == Axis.Y
                && isOtherRailInAxis(level, pos, Axis.X, -1) != TriState.TRUE
                && isOtherRailInAxis(level, pos, Axis.X, 1) != TriState.TRUE
            ) {
                level.setBlockAndUpdate(pos, state.setValue(AXIS, Axis.Z));
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private TriState isOtherRailInAxis(Level level, BlockPos pos, Axis axis, int relative) {
        BlockState other = level.getBlockState(pos.relative(axis, relative));
        Axis otherAxis;
        if (other.getBlock() instanceof SlidingRailBlock) {
            otherAxis = other.getValue(AXIS);
        } else if (other.getBlock() instanceof PoweredSlidingRailBlock) {
            otherAxis = other.getValue(PoweredSlidingRailBlock.FACING).getAxis();
        } else if (other.getBlock() instanceof ActivatorSlidingRailBlock) {
            otherAxis = other.getValue(ActivatorSlidingRailBlock.FACING).getAxis();
        } else if (other.getBlock() instanceof DetectorSlidingRailBlock) {
            otherAxis = other.getValue(DetectorSlidingRailBlock.FACING).getAxis();
        } else {
            return TriState.DEFAULT;
        }
        return axis == otherAxis || otherAxis == Axis.Y ? TriState.TRUE : TriState.FALSE;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        BlockState bs = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, bs.cycle(AXIS));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return AXIS;
    }

    @Override
    public void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity) {
    }
}
