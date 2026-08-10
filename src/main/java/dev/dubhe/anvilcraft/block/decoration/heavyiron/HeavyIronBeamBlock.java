package dev.dubhe.anvilcraft.block.decoration.heavyiron;

import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class HeavyIronBeamBlock extends Block implements IHammerRemovable, HammerRotateBehavior {

    public static final VoxelShape AABB_X =
        Shapes.join(Block.box(0, 12, 0, 16, 16, 16), Block.box(0, 0, 4, 16, 12, 12), BooleanOp.OR);
    public static final VoxelShape AABB_Z =
        Shapes.join(Block.box(0, 12, 0, 16, 16, 16), Block.box(4, 0, 0, 12, 12, 16), BooleanOp.OR);

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public HeavyIronBeamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(HeavyIronBeamBlock.AXIS, Direction.Axis.X));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(HeavyIronBeamBlock.AXIS, context.getHorizontalDirection().getOpposite().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HeavyIronBeamBlock.AXIS);
    }

    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext
    ) {
        if (blockState.getValue(HeavyIronBeamBlock.AXIS) == Direction.Axis.X) {
            return HeavyIronBeamBlock.AABB_X;
        } else {
            return HeavyIronBeamBlock.AABB_Z;
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(HeavyIronBeamBlock.AXIS)) {
                case Z -> state.setValue(HeavyIronBeamBlock.AXIS, Direction.Axis.X);
                case X -> state.setValue(HeavyIronBeamBlock.AXIS, Direction.Axis.Z);
                default -> state;
            };
            default -> state;
        };
    }
}
