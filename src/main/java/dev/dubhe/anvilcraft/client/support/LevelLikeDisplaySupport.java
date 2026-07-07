package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class LevelLikeDisplaySupport {

    // @OnlyIn(Dist.CLIENT)
    public static LevelLike asLevelLike(BlockPattern pattern) {
        @SuppressWarnings("DataFlowIssue")
        LevelLike levelLike = new LevelLike(Minecraft.getInstance().level);

        int size = pattern.getSize();
        for (int y = size - 1; y >= 0; y--) {
            for (int x = size - 1; x >= 0; x--) {
                for (int z = size - 1; z >= 0; z--) {
                    BlockPredicateWithState predicate = pattern.getPredicate(x, y, z);
                    BlockState state = predicate.getDefaultState();
                    if (state.isAir() && Math.max(levelLike.horizontalSize(), levelLike.verticalSize()) >= size) {
                        continue;
                    }
                    levelLike.setBlockState(new BlockPos(x - size / 2, y - size / 2, z - size / 2), state);
                }
            }
        }

        return levelLike;
    }
}
