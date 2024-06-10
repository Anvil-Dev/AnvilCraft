package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.MultiplePartBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMultiplePartBlock<P extends Enum<P>> extends Block {
    public AbstractMultiplePartBlock(Properties properties) {
        super(properties);
    }

    protected abstract Property<P> getPart();

    protected abstract Enum<P>[] getParts();

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(
        @NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
        @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos
    ) {
        if (
            !state.hasProperty(this.getPart())
                || !(state.getValue(this.getPart()) instanceof MultiplePartBlockState state1)
        ) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
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
}
