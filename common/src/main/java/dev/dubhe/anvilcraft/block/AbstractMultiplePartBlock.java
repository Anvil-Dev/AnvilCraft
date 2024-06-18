package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.MultiplePartBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMultiplePartBlock<P extends Enum<P> & MultiplePartBlockState<P>> extends Block {
    public AbstractMultiplePartBlock(Properties properties) {
        super(properties);
    }

    public abstract @NotNull Property<P> getPart();

    public abstract P[] getParts();

    protected Vec3i getMainPartOffset() {
        return new Vec3i(0, 0, 0);
    }

    @Override
    public final void setPlacedBy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
        @Nullable LivingEntity placer, @NotNull ItemStack stack
    ) {
        if (!state.hasProperty(this.getPart())) return;
        for (P part : this.getParts()) {
            if (part.getOffset().distSqr(new Vec3i(0, 0, 0)) <= 0) continue;
            BlockPos blockPos = pos.offset(part.getOffset());
            BlockState newState = this.placedState(part, state);
            level.setBlockAndUpdate(blockPos, newState);
        }
    }

    protected BlockState placedState(P part, @NotNull BlockState state) {
        return state.setValue(this.getPart(), part);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(
        @NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
        @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos
    ) {
        if (!state.hasProperty(this.getPart())) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        MultiplePartBlockState<P> state1 = state.getValue(this.getPart());
        for (P part : getParts()) {
            Vec3i offset = neighborPos.subtract(pos).offset(state1.getOffset()); // 更新来源偏移值
            if (offset.distSqr(part.getOffset()) != 0) continue;
            if (
                !neighborState.is(this)
                    || !neighborState.hasProperty(this.getPart())
                    || neighborState.getValue(this.getPart()) != part
            ) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player
    ) {
        if (!level.isClientSide && player.isCreative()) {
            this.preventCreativeDropFromMainPart(level, pos, state, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private void preventCreativeDropFromMainPart(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player
    ) {
        if (!state.is(this)) return;
        if (!state.hasProperty(this.getPart())) return;
        P value = state.getValue(this.getPart());
        if (value.getOffset().distSqr(this.getMainPartOffset()) == 0) return;
        BlockPos mainPartPos = pos.subtract(value.getOffset()).offset(this.getMainPartOffset());
        BlockState mainPartState = level.getBlockState(mainPartPos);
        if (!mainPartState.is(this)) return;
        if (!mainPartState.hasProperty(this.getPart())) return;
        BlockState blockState2 = mainPartState.getFluidState().is(Fluids.WATER)
            ? Blocks.WATER.defaultBlockState()
            : Blocks.AIR.defaultBlockState();
        level.setBlock(mainPartPos, blockState2, 35);
        level.levelEvent(player, 2001, mainPartPos, Block.getId(mainPartState));
    }

    /**
     * 获取多方块战利品表
     *
     * @param provider 提供器
     * @param block    方块
     */
    public static <T extends Enum<T> & MultiplePartBlockState<T>> void loot(
        BlockLootSubProvider provider, @NotNull AbstractMultiplePartBlock<T> block
    ) {
        for (T part : block.getParts()) {
            if (part.getOffset().distSqr(block.getMainPartOffset()) == 0) {
                provider.add(block, provider.createSinglePropConditionTable(block, block.getPart(), part));
                break;
            }
        }
    }

    @Nullable
    @Override
    public final BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        if (!hasEnoughSpace(context.getClickedPos(), context.getLevel())) return null; // 判断是否有足够空间放置方块
        return this.getPlacementState(context);
    }

    @Nullable
    public BlockState getPlacementState(@NotNull BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    /**
     * 是否有足够的空间放下方块
     */
    public boolean hasEnoughSpace(BlockPos pos, LevelReader level) {
        for (P part : getParts()) {
            BlockPos pos1 = pos.offset(part.getOffset());
            if (level.isOutsideBuildHeight(pos1)) return false;
            BlockState state = level.getBlockState(pos1);
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }
}
