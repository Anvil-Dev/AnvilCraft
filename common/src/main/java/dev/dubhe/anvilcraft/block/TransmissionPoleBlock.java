package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.state.Half;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class TransmissionPoleBlock extends BaseEntityBlock implements IHammerRemovable {
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    /**
     * @param properties 属性
     */
    public TransmissionPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, Half.BOTTOM)
                .setValue(OVERLOAD, true)
                .setValue(SWITCH, IPowerComponent.Switch.ON)
        );
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(HALF, Half.BOTTOM)
            .setValue(OVERLOAD, true)
            .setValue(SWITCH, IPowerComponent.Switch.ON);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF).add(OVERLOAD).add(SWITCH);
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
        LivingEntity placer, @NotNull ItemStack stack
    ) {
        BlockPos above = pos.above();
        level.setBlockAndUpdate(above, state.setValue(HALF, Half.MID));
        above = above.above();
        level.setBlockAndUpdate(above, state.setValue(HALF, Half.TOP));
    }

    @Override
    public void playerWillDestroy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player
    ) {
        if (!level.isClientSide) {
            TransmissionPoleBlock.preventDropFromOtherPart(level, pos, state, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private static void preventDropFromOtherPart(
        Level level, BlockPos pos, @NotNull BlockState state, Player player
    ) {
        BlockPos blockPos;
        BlockState blockState;
        BlockPos blockPos1;
        BlockState blockState1;
        Half half = state.getValue(HALF);
        if (
            half == Half.TOP
                && (blockState = level.getBlockState(blockPos = pos.below())).is(state.getBlock())
                && blockState.getValue(HALF) == Half.MID
                && (blockState1 = level.getBlockState(blockPos1 = blockPos.below())).is(state.getBlock())
                && blockState1.getValue(HALF) == Half.BOTTOM
        ) {
            breakOtherPart(level, blockPos, blockState, blockPos1, blockState1, player);
        } else if (
            half == Half.MID
                && (blockState = level.getBlockState(blockPos = pos.above())).is(state.getBlock())
                && blockState.getValue(HALF) == Half.TOP
                && (blockState1 = level.getBlockState(blockPos1 = pos.below())).is(state.getBlock())
                && blockState1.getValue(HALF) == Half.BOTTOM
        ) {
            breakOtherPart(level, blockPos, blockState, blockPos1, blockState1, player);
        } else if (
            half == Half.BOTTOM
                && (blockState = level.getBlockState(blockPos = pos.above())).is(state.getBlock())
                && blockState.getValue(HALF) == Half.MID
                && (blockState1 = level.getBlockState(blockPos1 = blockPos.above())).is(state.getBlock())
                && blockState1.getValue(HALF) == Half.TOP
        ) {
            breakOtherPart(level, blockPos, blockState, blockPos1, blockState1, player);
        }
    }

    private static void breakOtherPart(
        @NotNull Level level, BlockPos blockPos, @NotNull BlockState blockState,
        BlockPos blockPos1, @NotNull BlockState blockState1, Player player
    ) {
        BlockState blockState2 = Blocks.AIR.defaultBlockState();
        level.setBlock(blockPos, blockState2, 35);
        level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
        level.setBlock(blockPos1, blockState2, 35);
        level.levelEvent(player, 2001, blockPos1, Block.getId(blockState1));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TransmissionPoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.REMOTE_TRANSMISSION_POLE.get(),
            (level1, pos, state1, entity) -> entity.tick(level1, pos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(HALF) != Half.BOTTOM) return;
        IPowerComponent.Switch sw = state.getValue(SWITCH);
        boolean bl = sw != IPowerComponent.Switch.ON;
        BlockPos topPos = pos.above(2);
        BlockState topState = level.getBlockState(topPos);
        if (!topState.is(ModBlocks.TRANSMISSION_POLE.get())) return;
        if (topState.getValue(HALF) != Half.TOP) return;
        if (bl != level.hasNeighborSignal(pos)) {
            if (bl) {
                state.setValue(SWITCH, IPowerComponent.Switch.OFF);
                topState.setValue(SWITCH, IPowerComponent.Switch.OFF);
            } else {
                state.setValue(SWITCH, IPowerComponent.Switch.ON);
                topState.setValue(SWITCH, IPowerComponent.Switch.ON);
            }
            level.setBlockAndUpdate(pos, state);
            level.setBlockAndUpdate(topPos, topState);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        switch (state.getValue(HALF)) {
            case TOP -> {
                BlockState state1 = level.getBlockState(pos.below());
                BlockState state2 = level.getBlockState(pos.below(2));
                return state1.is(this) && state2.is(this);
            }
            case MID -> {
                BlockState state2 = level.getBlockState(pos.below());
                return state2.is(this);
            }
            default -> {
                return state.isFaceSturdy(level, pos.below(), Direction.UP);
            }
        }
    }
}
