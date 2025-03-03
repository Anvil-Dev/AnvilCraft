package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.IAffectRangeProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AffectRangeProviderImpl implements IAffectRangeProvider {
    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof IHasAffectRange;
    }

    @Override
    public VoxelShape affectRange(BlockEntity entity) {
        if (entity instanceof IHasAffectRange) {
            return Shapes.create(((IHasAffectRange) entity).shape());
        }
        return null;
    }

    @Override
    public int priority() {
        return 0;
    }
}
