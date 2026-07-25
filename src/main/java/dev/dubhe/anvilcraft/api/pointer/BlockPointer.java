package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class BlockPointer implements ITargetPointer {
    private final BlockPos pos;
    private final BlockState state;

    public BlockPointer(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
    }

    @Override
    public boolean isStillValid(Level level) {
        BlockState current = level.getBlockState(this.pos);
        if (!current.is(this.state.getBlock())) {
            return false;
        }
        for (Property<?> property : this.state.getValues().keySet()) {
            if (current.getOptionalValue(property).filter(value -> value.equals(this.state.getValue(property))).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        if (!this.isStillValid(level)) {
            return false;
        }

        BlockEntity entity = null;
        if (this.state.getBlock() instanceof IMoveableEntityBlock) {
            entity = level.getBlockEntity(this.pos);
            if (entity != null) {
                level.removeBlockEntity(this.pos);
            }
        }
        level.removeBlock(this.pos, true);

        BlockState state = this.state;
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        }
        level.setBlock(pos, state, 67);
        if (state.getBlock() instanceof IMoveableEntityBlock block && entity != null) {
            entity.worldPosition = pos;
            entity.clearRemoved();
            level.removeBlockEntity(pos);
            level.setBlockEntity(entity);
            block.notifyMoved(level, pos, state, entity);
        }
        level.neighborChanged(pos, state.getBlock(), pos);
    }

    @Override
    public Type getType() {
        return null;
    }

    public static class Type implements ITargetPointer.Type<BlockPointer> {

        @Override
        public MapCodec<BlockPointer> codec() {
            return null;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockPointer> streamCodec() {
            return null;
        }

        @Override
        public @Nullable BlockPointer point(Level level, BlockPos pos, Direction facing) {
            BlockState state = level.getBlockState(pos);
            if (!PistonBaseBlock.isPushable(state, level, pos, facing.getOpposite(), false, facing.getOpposite())) {
                return null;
            }
            return new BlockPointer(pos, state);
        }
    }
}
