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
