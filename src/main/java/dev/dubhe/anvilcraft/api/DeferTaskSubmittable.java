package dev.dubhe.anvilcraft.api;

import java.util.function.Consumer;

public interface DeferTaskSubmittable<T> {
    void submitTask(Consumer<T> fn);
}
