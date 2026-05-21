package dev.dubhe.anvilcraft.api.injection.entity;

public interface ILivingEntityExtension {
    default void anvilcraft$setRaged() {
        throw new AssertionError();
    }
}
