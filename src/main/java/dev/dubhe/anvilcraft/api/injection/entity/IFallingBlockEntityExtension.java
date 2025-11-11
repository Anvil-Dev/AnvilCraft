package dev.dubhe.anvilcraft.api.injection.entity;

public interface IFallingBlockEntityExtension {
    default float anvilcraft$getFallDistance() {
        throw new AssertionError();
    }

    default boolean anvilcraft$isSpectral() {
        return false;
    }
}
