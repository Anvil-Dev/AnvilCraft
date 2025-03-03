package dev.dubhe.anvilcraft.util;

public class WatchablePropertyDelegate<T> {

    protected T value = null;

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

    protected void setValue(T newValue) {
        this.value = newValue;
    }
}
