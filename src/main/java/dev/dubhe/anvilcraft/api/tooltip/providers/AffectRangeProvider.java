package dev.dubhe.anvilcraft.api.tooltip.providers;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 作用范围提供器
 */
public interface AffectRangeProvider {
    boolean accepts(BlockEntity entity);

    VoxelShape affectRange(BlockEntity entity);

    int priority();
}
