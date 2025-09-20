package dev.dubhe.anvilcraft.util.function;

import java.util.Objects;
import java.util.function.Function;

public interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);

    default <R2> TriFunction<T, U, V, R2> andThen(Function<? super R, ? extends R2> after) {
        Objects.requireNonNull(after);
        return (T t, U u, V v) -> after.apply(apply(t, u, v));
    }
}
