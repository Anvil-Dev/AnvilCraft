package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class IncandescentBlock extends RedhotBlock {
    public IncandescentBlock(Properties properties) {
        super(properties, 4, 8);
    }

    @Override
    public HeatableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.INCANDESCENT_BLOCK.create(pos, state);
    }
}
