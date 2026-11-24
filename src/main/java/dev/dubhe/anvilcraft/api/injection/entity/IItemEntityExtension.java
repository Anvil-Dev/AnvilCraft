package dev.dubhe.anvilcraft.api.injection.entity;

import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;

public interface IItemEntityExtension {
    /** 服务端确认吸附后，将物品固定到节点并通知追踪该物品的客户端。 */
    default void anvilcraft$attachToMagnetizedNode(MagnetizedNodeEntity node) {
        throw new AssertionError();
    }

    default void anvilcraft$setIsAdsorbable(boolean value) {
        throw new AssertionError();
    }

    default boolean anvilcraft$isAdsorbable() {
        throw new AssertionError();
    }

    default boolean anvilcraft$getDiscarded() {
        throw new AssertionError();
    }

    default void anvilcraft$setMergeCooldown(int cooldown) {
        throw new AssertionError();
    }

    default void anvilcraft$setShouldPoach(boolean shouldPoach) {
        throw new UnsupportedOperationException("anvilcraft$setShouldPoach");
    }

    default void anvilcraft$poach() {
        throw new UnsupportedOperationException("anvilcraft$poach");
    }
}
