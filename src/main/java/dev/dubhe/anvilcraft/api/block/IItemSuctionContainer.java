package dev.dubhe.anvilcraft.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IItemSuctionContainer {
    static boolean allowsSuction(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof IItemSuctionContainer
            || level.getBlockEntity(pos) instanceof IItemSuctionContainer;
    }
}
