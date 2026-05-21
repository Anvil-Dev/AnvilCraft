package dev.dubhe.anvilcraft.api.injection;

public interface IItemSubmitExtension {
    default void anvilcraft$setHalfTransparent(boolean halfTransparent) {
        throw new AssertionError();
    }

    default boolean anvilcraft$isHalfTransparent() {
        throw new AssertionError();
    }
}
