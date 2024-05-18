package dev.dubhe.anvilcraft.util;

import java.util.function.Supplier;

public class Lazy<T> {
    private T instance;
    public final Supplier<T> supplier;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        synchronized (supplier) {
            if (instance == null) {
                instance = supplier.get();
            }
            return instance;
        }
    }
}
