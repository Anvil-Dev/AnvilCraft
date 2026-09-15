package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.stream.Stream;

public class LargeCakeBlock extends Block {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    protected static final VoxelShape BASE_ANGLE_NW = Stream.of(
            Block.box(1, 0, 1, 16, 6, 16), Block.box(0, 6, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape BASE_ANGLE_SW = Stream.of(
            Block.box(1, 0, 0, 16, 6, 15), Block.box(0, 6, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape BASE_ANGLE_SE = Stream.of(
            Block.box(0, 0, 0, 15, 6, 15), Block.box(0, 6, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape BASE_ANGLE_NE = Stream.of(
            Block.box(0, 0, 1, 15, 6, 16), Block.box(0, 6, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    protected static final VoxelShape BASE_N = Stream.of(Block.box(0, 6, 0, 16, 16, 16), Block.box(0, 0, 1, 16, 6, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape BASE_W = Stream.of(Block.box(0, 6, 0, 16, 16, 16), Block.box(1, 0, 0, 16, 6, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape BASE_S = Stream.of(Block.box(0, 6, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 6, 15))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape BASE_E = Stream.of(Block.box(0, 6, 0, 16, 16, 16), Block.box(0, 0, 0, 15, 6, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    protected static final VoxelShape MID_CENTER = Block.box(1, 0, 1, 15, 14, 15);

    protected static final VoxelShape MID_ANGLE_NW = Stream.of(
            Block.box(5, 5, 5, 17, 14, 17), Block.box(6, 0, 6, 17, 5, 17))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape MID_ANGLE_SW = Stream.of(
            Block.box(5, 5, -1, 17, 14, 11), Block.box(6, 0, -1, 17, 5, 10))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape MID_ANGLE_SE = Stream.of(
            Block.box(-1, 5, -1, 11, 14, 11), Block.box(-1, 0, -1, 10, 5, 10))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape MID_ANGLE_NE = Stream.of(
            Block.box(-1, 5, 5, 11, 14, 17), Block.box(-1, 0, 6, 10, 5, 17))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    protected static final VoxelShape MID_N = Stream.of(Block.box(1, 5, 5, 15, 14, 17), Block.box(1, 0, 6, 15, 5, 17))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape MID_W = Stream.of(Block.box(5, 5, 1, 17, 14, 15), Block.box(6, 0, 1, 17, 5, 15))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape MID_S = Stream.of(Block.box(1, 5, -1, 15, 14, 11), Block.box(1, 0, -1, 15, 5, 10))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape MID_E = Stream.of(Block.box(-1, 5, 1, 11, 14, 15), Block.box(-1, 0, 1, 10, 5, 15))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    protected static final VoxelShape TOP_CENTER = Stream.of(
            Block.box(5, 10, 5, 11, 13, 11), Block.box(2, -2, 2, 14, 10, 14))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    protected static final VoxelShape TOP_ANGLE_NW = Stream.of(
            Block.box(11, -2, 11, 18, 2, 18), Block.box(10, 2, 10, 18, 10, 18))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape TOP_ANGLE_SW = Stream.of(
            Block.box(11, -2, -2, 18, 2, 5), Block.box(10, 2, -2, 18, 10, 6))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape TOP_ANGLE_SE = Stream.of(
            Block.box(-2, -2, -2, 5, 2, 5), Block.box(-2, 2, -2, 6, 10, 6))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape TOP_ANGLE_NE = Stream.of(
            Block.box(-2, -2, 11, 5, 2, 18), Block.box(-2, 2, 10, 6, 10, 18))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    protected static final VoxelShape TOP_N = Stream.of(
            Block.box(2, -2, 11, 14, 2, 18), Block.box(2, 2, 10, 14, 10, 18))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape TOP_W = Stream.of(
            Block.box(11, -2, 2, 18, 2, 14), Block.box(10, 2, 2, 18, 10, 14))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape TOP_S = Stream.of(Block.box(2, -2, -2, 14, 2, 5), Block.box(2, 2, -2, 14, 10, 6))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    protected static final VoxelShape TOP_E = Stream.of(Block.box(-2, -2, 2, 5, 2, 14), Block.box(-2, 2, 2, 6, 10, 14))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();

    public LargeCakeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getPartShape(state);
    }

    private VoxelShape getPartShape(BlockState state) {
        return switch (state.getValue(HALF)) {
            case TOP_CENTER -> TOP_CENTER;
            case TOP_E -> TOP_E;
            case TOP_W -> TOP_W;
            case TOP_N -> TOP_N;
            case TOP_S -> TOP_S;
            case TOP_EN -> TOP_ANGLE_NE;
            case TOP_ES -> TOP_ANGLE_SE;
            case TOP_WN -> TOP_ANGLE_NW;
            case TOP_WS -> TOP_ANGLE_SW;
            case MID_CENTER -> MID_CENTER;
            case MID_E -> MID_E;
            case MID_W -> MID_W;
            case MID_N -> MID_N;
            case MID_S -> MID_S;
            case MID_EN -> MID_ANGLE_NE;
            case MID_ES -> MID_ANGLE_SE;
            case MID_WN -> MID_ANGLE_NW;
            case MID_WS -> MID_ANGLE_SW;
            case BOTTOM_E -> BASE_E;
            case BOTTOM_W -> BASE_W;
            case BOTTOM_N -> BASE_N;
            case BOTTOM_S -> BASE_S;
            case BOTTOM_EN -> BASE_ANGLE_NE;
            case BOTTOM_ES -> BASE_ANGLE_SE;
            case BOTTOM_WN -> BASE_ANGLE_NW;
            case BOTTOM_WS -> BASE_ANGLE_SW;
            default -> Block.box(0, 1, 0, 16, 16, 16);
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        return this.use(level, pos, player, hand).consumesAction()
            ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this.use(level, pos, player, InteractionHand.MAIN_HAND);
    }

    private InteractionResult use(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (eat(level, pos, player).consumesAction()) {
                return InteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }

        return eat(level, pos, player);
    }

    private static InteractionResult eat(Level level, BlockPos pos, Player player) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        } else {
            player.getFoodData().eat(15, 0.8f);
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror));
    }
}
