package dev.dubhe.anvilcraft.util;

import java.util.Objects;

@FunctionalInterface
public interface TriConsumer<F, S, T> {
    void accept(F f, S s, T t);

    default TriConsumer<F, S, T> andThen(TriConsumer<F, S, T> after) {
        Objects.requireNonNull(after);
        return (f, s, t) -> {
            accept(f, s, t);
            after.accept(f, s, t);
        };
    }
}
