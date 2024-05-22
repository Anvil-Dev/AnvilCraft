package dev.dubhe.anvilcraft.util;

public abstract class WatchablePropertyDelegate<T> {

    private T value = null;

    public WatchablePropertyDelegate(T value) {
        this.value = value;
    }

    public WatchablePropertyDelegate() {
    }

    protected abstract void onChanged(T oldValue, T newValue);

    public T get() {
        return value;
    }

    public void set(T newValue) {
        if (value != newValue) onChanged(value, newValue);
        this.value = newValue;
    }
}
