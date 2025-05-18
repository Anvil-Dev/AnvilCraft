package dev.dubhe.anvilcraft.api;

import java.util.function.Consumer;

public interface DeferTaskSubmittable<T> {
    void anvilcraft$submitTask(Consumer<T> fn);
}
