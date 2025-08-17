package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class GlowingBlock extends RedhotBlock {
    public GlowingBlock(Properties properties) {
        super(properties, 2, 6);
    }

    @Override
    public HeatableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.GLOWING_BLOCK.create(pos, state);
    }
}
