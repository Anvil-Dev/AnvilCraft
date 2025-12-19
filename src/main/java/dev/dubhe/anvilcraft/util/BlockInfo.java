package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record BlockInfo(BlockPos pos, BlockState state) {
}
