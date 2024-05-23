package dev.dubhe.anvilcraft.api.property;

import java.util.UUID;

public class WatchablePropertyDelegate<T> {

    protected T value = null;
    private final UUID syncId = UUID.randomUUID();

    public WatchablePropertyDelegate(T value) {
        this.value = value;
    }

    public WatchablePropertyDelegate() {
    }

    protected void onChanged(T oldValue, T newValue) {

    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        if (value != newValue) onChanged(value, newValue);
        this.value = newValue;
    }
}
