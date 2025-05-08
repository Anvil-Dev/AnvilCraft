package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;

import java.util.Collection;

/**
 * 用于设置爆炸方块转换
 */
public interface BlockTransformExplosion {
    default void setBlockTransformExplosion(@SuppressWarnings("unused") Collection<BlockTransform> blockTransformExplosions) {
    }
}
