package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class BlueprintBlockEntities {
    private BlueprintBlockEntities() {
    }

    @Nullable
    public static BlockEntity create(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag data) {
        BlockEntity entity = state.is(Blocks.MOVING_PISTON) ? new PistonMovingBlockEntity(pos, state)
            : state.getBlock() instanceof EntityBlock block ? block.newBlockEntity(pos, state) : null;
        if (entity == null) return null;
        if (data != null) entity.loadWithComponents(data, level.registryAccess());
        entity.setLevel(level);
        return entity;
    }
}
