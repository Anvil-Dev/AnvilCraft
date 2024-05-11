package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.network.ServerboundCyclingValueSyncPacket;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class WatchableCyclingValue<T> {
    public final T[] values;
    private int index = 0;
    private final Consumer<WatchableCyclingValue<T>> onChangedCallback;
    @Getter
    private final String name;

    @SafeVarargs
    public WatchableCyclingValue(String name, Consumer<WatchableCyclingValue<T>> onChangedCallback, T... values) {
        this.onChangedCallback = onChangedCallback;
        this.values = values;
        this.name = name;
    }

    void onChanged() {
        onChangedCallback.accept(this);
    }

    public int count() {
        return values.length;
    }

    public WatchableCyclingValue<T> fromIndex(int index) {
        if (index >= values.length) {
            throw new IndexOutOfBoundsException(index);
        }
        this.index = index;
        onChanged();
        return this;
    }

    public int index() {
        return index;
    }

    public T get() {
        return values[index];
    }

    public T next() {
        if (index + 1 >= values.length) {
            index = 0;
            return values[index];
        }
        onChanged();
        return values[index++];
    }

    public T previous() {
        if (index - 1 < 0) {
            index = values.length - 1;
            return values[index];
        }
        onChanged();
        return values[index--];
    }

    public void notifyServer() {
        new ServerboundCyclingValueSyncPacket(index, name).send();
    }
}
