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
import org.jetbrains.annotations.NotNull;

public class LargeCakeBlock extends AbstractMultiplePartBlock<Cube3x3PartHalf> {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

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

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @NotNull Property<Cube3x3PartHalf> getPart() {
        return LargeCakeBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
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
    public void playerWillDestroy(
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
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @SuppressWarnings("deprecation")
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
