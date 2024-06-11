package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.MultiplePartBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMultiplePartBlock
    <P extends Enum<P> & Comparable<P> & StringRepresentable & MultiplePartBlockState>
    extends Block {
    public AbstractMultiplePartBlock(Properties properties) {
        super(properties);
    }

    protected abstract @NotNull Property<P> getPart();

    protected abstract Enum<P>[] getParts();

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(
        @NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
        @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos
    ) {
        MultiplePartBlockState state1;
        if (!state.hasProperty(this.getPart())) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        state1 = state.getValue(this.getPart());
        for (Enum<?> part : getParts()) {
            if (part instanceof MultiplePartBlockState state2) {
                Vec3i offset = state2.getOffset().subtract(state1.getOffset());
                int distance = offset.getX() + offset.getY() + offset.getZ();
                if (Math.abs(distance) != 1) continue;
                BlockPos pos1 = pos.offset(offset);
                BlockState blockState = level.getBlockState(pos1);
                if (
                    !blockState.is(this)
                        || !blockState.hasProperty(this.getPart())
                        || blockState.getValue(this.getPart()) != state2
                ) {
                    return Blocks.AIR.defaultBlockState();
                }
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
        if (value.getOffset().distSqr(new Vec3i(0, 0, 0)) == 0) return;
        BlockPos mainPartPos = pos.subtract(value.getOffset());
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
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & Comparable<T> & StringRepresentable & MultiplePartBlockState> void loot(
        BlockLootSubProvider provider, @NotNull AbstractMultiplePartBlock<T> block
    ) {
        for (Enum<?> part : block.getParts()) {
            if (!(part instanceof MultiplePartBlockState state)) continue;
            if (state.getOffset().distSqr(new Vec3i(0, 0, 0)) == 0) {
                provider.add(block, provider.createSinglePropConditionTable(block, block.getPart(), (T) part));
                break;
            }
        }
    }
}
