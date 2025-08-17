package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class HeatedBlock extends HeatableBlock implements IMoveableEntityBlock {
    public HeatedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasBlockEntity() {
        return true;
    }

    @Override
    public HeatableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HEATED_BLOCK.create(pos, state);
    }
}
