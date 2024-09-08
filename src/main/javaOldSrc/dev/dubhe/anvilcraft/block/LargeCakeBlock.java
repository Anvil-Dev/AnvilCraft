package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class LargeCakeBlock extends AbstractMultiplePartBlock<Cube3x3PartHalf> {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    protected static final VoxelShape BASE_ANGLE_NW = Stream.of(
        Block.box(1, 0, 1, 16, 6, 16), Block.box(0, 6, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_ANGLE_SW = Stream.of(
        Block.box(1, 0, 0, 16, 6, 15), Block.box(0, 6, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_ANGLE_SE = Stream.of(
        Block.box(0, 0, 0, 15, 6, 15), Block.box(0, 6, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_ANGLE_NE = Stream.of(
        Block.box(0, 0, 1, 15, 6, 16), Block.box(0, 6, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    protected static final VoxelShape BASE_N = Stream.of(
        Block.box(0, 6, 0, 16, 16, 16), Block.box(0, 0, 1, 16, 6, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_W = Stream.of(
        Block.box(0, 6, 0, 16, 16, 16), Block.box(1, 0, 0, 16, 6, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_S = Stream.of(
        Block.box(0, 6, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 6, 15)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_E = Stream.of(
        Block.box(0, 6, 0, 16, 16, 16), Block.box(0, 0, 0, 15, 6, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    protected static final VoxelShape MID_CENTER = Block.box(1, 0, 1, 15, 14, 15);

    protected static final VoxelShape MID_ANGLE_NW = Stream.of(
        Block.box(5, 5, 5, 17, 14, 17), Block.box(6, 0, 6, 17, 5, 17)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_ANGLE_SW = Stream.of(
        Block.box(5, 5, -1, 17, 14, 11), Block.box(6, 0, -1, 17, 5, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_ANGLE_SE = Stream.of(
        Block.box(-1, 5, -1, 11, 14, 11), Block.box(-1, 0, -1, 10, 5, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_ANGLE_NE = Stream.of(
        Block.box(-1, 5, 5, 11, 14, 17), Block.box(-1, 0, 6, 10, 5, 17)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();


    protected static final VoxelShape MID_N = Stream.of(
        Block.box(1, 5, 5, 15, 14, 17), Block.box(1, 0, 6, 15, 5, 17)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_W = Stream.of(
        Block.box(5, 5, 1, 17, 14, 15), Block.box(6, 0, 1, 17, 5, 15)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_S = Stream.of(
        Block.box(1, 5, -1, 15, 14, 11), Block.box(1, 0, -1, 15, 5, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_E = Stream.of(
        Block.box(-1, 5, 1, 11, 14, 15), Block.box(-1, 0, 1, 10, 5, 15)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    protected static final VoxelShape TOP_CENTER = Stream.of(
        Block.box(5, 10, 5, 11, 13, 11), Block.box(2, -2, 2, 14, 10, 14)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    protected static final VoxelShape TOP_ANGLE_NW = Stream.of(
        Block.box(11, -2, 11, 18, 2, 18), Block.box(10, 2, 10, 18, 10, 18)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape TOP_ANGLE_SW = Stream.of(
        Block.box(11, -2, -2, 18, 2, 5), Block.box(10, 2, -2, 18, 10, 6)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape TOP_ANGLE_SE = Stream.of(
        Block.box(-2, -2, -2, 5, 2, 5), Block.box(-2, 2, -2, 6, 10, 6)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape TOP_ANGLE_NE = Stream.of(
        Block.box(-2, -2, 11, 5, 2, 18), Block.box(-2, 2, 10, 6, 10, 18)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    protected static final VoxelShape TOP_N = Stream.of(
        Block.box(2, -2, 11, 14, 2, 18), Block.box(2, 2, 10, 14, 10, 18)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape TOP_W = Stream.of(
        Block.box(11, -2, 2, 18, 2, 14), Block.box(10, 2, 2, 18, 10, 14)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape TOP_S = Stream.of(
        Block.box(2, -2, -2, 14, 2, 5), Block.box(2, 2, -2, 14, 10, 6)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape TOP_E = Stream.of(
        Block.box(-2, -2, 2, 5, 2, 14), Block.box(-2, 2, 2, 6, 10, 14)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    /**
     * @param properties 属性
     */
    public LargeCakeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER)
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
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
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @NotNull Property<Cube3x3PartHalf> getPart() {
        return LargeCakeBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf @NotNull [] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos
    ) {
        return true;
    }

    @Override
    public void onPlace(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
        @NotNull BlockState oldState, boolean movedByPiston
    ) {
        if (state.getValue(HALF) != Cube3x3PartHalf.BOTTOM_CENTER) return;
        for (Cube3x3PartHalf part : this.getParts()) {
            if (part == Cube3x3PartHalf.BOTTOM_CENTER) continue;
            BlockState newState = state
                .setValue(HALF, part);
            level.setBlockAndUpdate(pos.offset(part.getOffset()), newState);
        }
    }

    @Override
    public @NotNull BlockState updateShape(
        @NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
        @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos
    ) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        this.spawnDestroyParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS)) {
            PiglinAi.angerNearbyPiglins(player, false);
        }

        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
        @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit
    ) {
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
            removeFromTop(level, pos, player);
            return InteractionResult.SUCCESS;
        }
    }

    private static void removeFromTop(Level level, BlockPos pos, Player player) {
        BlockState aboveState = level.getBlockState(pos.above());
        if (aboveState.getBlock() instanceof LargeCakeBlock && aboveState.getValue(HALF).getOffsetY() != 0) {
            removeFromTop(level, pos.above(), player);
            return;
        }
        level.removeBlock(pos, false);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
    }
}
