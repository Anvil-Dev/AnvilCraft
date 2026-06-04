package dev.dubhe.anvilcraft.api.injection;

import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;

import java.util.Collection;

/// 用于设置爆炸方块转换
public interface IExplosionExtension {
    default void anvilcraft$setBlockTransformExplosion(Collection<BlockTransform> blockTransformExplosions) {
        throw new AssertionError();
    }
}
