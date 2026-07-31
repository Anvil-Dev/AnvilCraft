package dev.dubhe.anvilcraft.util;

import org.jspecify.annotations.Nullable;

public class WatchablePropertyDelegate<T> {

    protected @Nullable T value;

    public WatchablePropertyDelegate(T value) {
        this.value = value;
    }

    public WatchablePropertyDelegate() {
    }

    protected void onChanged(@Nullable T oldValue, T newValue) {
    }

    public @Nullable T get() {
        return this.value;
    }

    public void set(T newValue) {
        if (this.value != newValue) this.onChanged(this.value, newValue);
        this.value = newValue;
    }

    protected void setValue(T newValue) {
        this.value = newValue;
    }
}
