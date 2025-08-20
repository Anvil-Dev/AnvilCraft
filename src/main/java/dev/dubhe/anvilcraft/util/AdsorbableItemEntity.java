package dev.dubhe.anvilcraft.util;

public interface AdsorbableItemEntity {
    default void anvilcraft$setIsAdsorbable(boolean value) {
        throw new AssertionError();
    }

    default boolean anvilcraft$isAdsorbable() {
        throw new AssertionError();
    }
}
