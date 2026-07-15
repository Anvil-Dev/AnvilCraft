package dev.dubhe.anvilcraft.api.injection.entity;

public interface IExperienceOrbExtension {
    default boolean anvilcraft$getDiscarded() {
        throw new UnsupportedOperationException("anvilcraft$getDiscarded");
    }

    default void anvilcraft$poach() {
        throw new UnsupportedOperationException("anvilcraft$poach");
    }
}
