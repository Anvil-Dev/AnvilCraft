package dev.dubhe.anvilcraft.util;

public interface AdsorbableItemEntity {
    default void anvilcraft$setIsAdsorbable(boolean value) {
    }

    default boolean anvilcraft$isAdsorbable() {
        return true;
    }
}
