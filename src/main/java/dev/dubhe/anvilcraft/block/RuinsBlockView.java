package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

public record RuinsBlockView(BlockGetter level) implements BlockGetter {
    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins
            ? ruins.getDisplayState() : this.level.getBlockState(pos);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        BlockEntity entity = this.level.getBlockEntity(pos);
        return entity instanceof RuinsBlockEntity ruins ? ruins.getDisplayEntity() : entity;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }
}
