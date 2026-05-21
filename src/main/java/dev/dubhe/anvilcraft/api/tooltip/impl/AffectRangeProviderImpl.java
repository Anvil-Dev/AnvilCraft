package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.IAffectRangeProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AffectRangeProviderImpl implements IAffectRangeProvider {
    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof IHasAffectRange;
    }

    @Override
    public VoxelShape affectRange(BlockEntity entity) {
        // 有作用范围且作用范围不为 null
        if (entity instanceof IHasAffectRange har && har.shape() instanceof AABB shape) {
            return Shapes.create(shape);
        }
        return Shapes.empty();
    }

    @Override
    public int priority() {
        return 0;
    }
}
