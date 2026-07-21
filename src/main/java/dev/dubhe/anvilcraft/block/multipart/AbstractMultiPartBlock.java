package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMultiPartBlock<P extends Enum<P>> extends Block implements IMultiPartBlockModelHolder {
    public AbstractMultiPartBlock(Properties properties) {
        super(properties);
    }

    public abstract Property<P> getPart();

    public abstract P[] getParts();

    public abstract boolean isMainPart(BlockState state);

    public abstract BlockPos getMainPartPos(BlockPos pos, BlockState state);

    public abstract Vec3i offsetFrom(BlockState state, P part);

    public abstract Vec3i getOffset(BlockState state);

    public BlockState placedState(P part, BlockState state) {
        return state.setValue(this.getPart(), part);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        if (!state.hasProperty(this.getPart())) return;
        for (P part : this.getParts()) {
            if (part == state.getValue(this.getPart())) continue;
            BlockPos blockPos = pos.offset(this.offsetFrom(state, part));
            BlockState newState = this.placedState(part, state);
            level.setBlockAndUpdate(blockPos, newState);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        if (!state.hasProperty(this.getPart())) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        Vec3i neighborOffset = neighbourPos.subtract(pos);
        for (P part : this.getParts()) {
            Vec3i offset = this.offsetFrom(state, part); // 更新来源偏移值
            if (!offset.equals(neighborOffset)) continue;
            if (!neighbourState.is(this)
                || !neighbourState.hasProperty(this.getPart())
                || neighbourState.getValue(this.getPart()) != part) {
                return state.getFluidState().createLegacyBlock();
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!level.isClientSide() && player.isCreative()) {
            this.preventCreativeDropFromMainPart(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected void preventCreativeDropFromMainPart(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!state.is(this)) return;
        if (!state.hasProperty(this.getPart())) return;
        if (this.isMainPart(state)) return;
        BlockPos mainPartPos = this.getMainPartPos(pos, state);
        BlockState mainPartState = level.getBlockState(mainPartPos);
        if (!mainPartState.is(this)) return;
        if (!mainPartState.hasProperty(this.getPart())) return;
        BlockState blockState2 = mainPartState.getFluidState().createLegacyBlock();
        level.setBlock(mainPartPos, blockState2, 35);
        level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, mainPartPos, Block.getId(mainPartState));
    }

    public void removePartsAndUpdate(Level level, BlockPos pos) {
        BlockState baseState = level.getBlockState(pos);
        for (P part : this.getParts()) {
            BlockPos bp = pos.offset(this.offsetFrom(baseState, part));
            BlockState blockState = level.getBlockState(bp);
            level.setBlock(bp, blockState.getFluidState().createLegacyBlock(), 3, 0);
        }
    }
}
