package dev.dubhe.anvilcraft.api.injection.entity;

public interface IExperienceOrbExtension {
    default void anvilcraft$setShouldPoach(boolean shouldPoach) {
        throw new UnsupportedOperationException("anvilcraft$setShouldPoach");
    }

    default void anvilcraft$poach() {
        throw new UnsupportedOperationException("anvilcraft$poach");
    }
}
