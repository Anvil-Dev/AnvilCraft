package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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

    protected BlockState placedState(P part, BlockState state) {
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
    public BlockState updateShape(
        BlockState state,
        Direction direction,
        BlockState neighborState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos) {
        if (!state.hasProperty(this.getPart())) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        Vec3i neighborOffset = neighborPos.subtract(pos);
        for (P part : getParts()) {
            Vec3i offset = this.offsetFrom(state, part); // 更新来源偏移值
            if (!offset.equals(neighborOffset)) continue;
            if (!neighborState.is(this)
                || !neighborState.hasProperty(this.getPart())
                || neighborState.getValue(this.getPart()) != part) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!level.isClientSide && player.isCreative()) {
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

}
