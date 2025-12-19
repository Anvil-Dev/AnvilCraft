package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IMultiPartBlockModelHolder {
    default BlockState mapRealModelHolderBlock(Level level, BlockPos blockPos, BlockState original) {
        return original;
    }
}
