package dev.dubhe.anvilcraft.block.heatable;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @Override
    public void notifyMoved(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        Util.<HeatableBlockEntity>cast(be).setDuration(0);
    }
}
