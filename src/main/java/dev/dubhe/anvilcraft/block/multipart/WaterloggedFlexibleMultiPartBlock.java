package dev.dubhe.anvilcraft.block.multipart;

import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.dubhe.anvilcraft.block.state.IFlexibleMultiPartBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public abstract class WaterloggedFlexibleMultiPartBlock<
    P extends Enum<P> & IFlexibleMultiPartBlockState<P, E>,
    T extends Property<E>,
    E extends Comparable<E>
    > extends FlexibleMultiPartBlock<P, T, E> implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected WaterloggedFlexibleMultiPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    protected final BlockState waterloggedStateForPlacement(BlockPlaceContext context, BlockState state) {
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        for (P part : this.getParts()) {
            if (part == state.getValue(this.getPart())) continue;
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            BlockState partState = this.placedState(part, state)
                .setValue(WATERLOGGED, level.getFluidState(partPos).is(Fluids.WATER));
            level.setBlockAndUpdate(partPos, partState);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public <J extends Property<H>, H extends Comparable<H>> void updateState(
        Level level,
        BlockPos pos,
        J property,
        H value,
        int flag
    ) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        for (P part : this.getParts()) {
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) level.setBlock(partPos, partState.setValue(property, value), flag);
        }
    }

    @Override
    public void change(BlockPos pos, Level level, NonNullFunction<BlockState, BlockState> factory) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        BlockState changedState = factory.apply(state);
        for (P part : this.getParts()) {
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            BlockState partState = level.getBlockState(partPos);
            if (!partState.is(this)) continue;
            BlockState changedPartState = this.placedState(part, changedState)
                .setValue(WATERLOGGED, partState.getValue(WATERLOGGED));
            level.setBlockAndUpdate(partPos, changedPartState);
        }
    }
}
