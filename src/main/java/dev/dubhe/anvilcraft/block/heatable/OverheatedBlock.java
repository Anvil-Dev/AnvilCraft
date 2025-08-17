package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class OverheatedBlock extends RedhotBlock {
    public OverheatedBlock(Properties properties) {
        super(properties, 8, 10);
    }

    @Override
    public HeatableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.OVERHEATED_BLOCK.create(pos, state);
    }
}
