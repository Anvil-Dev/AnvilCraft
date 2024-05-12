package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.network.ServerboundCyclingValueSyncPacket;
import lombok.Getter;

import java.util.function.Consumer;

public class WatchableCyclingValue<T> {
    public final T[] values;
    private int index = 0;
    private final Consumer<WatchableCyclingValue<T>> onChangedCallback;
    @Getter
    private final String name;

    /**
     * 可监听的循环值选择器
     */
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

    /**
     * 设置当前index
     *
     * @return this
     */
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

    /**
     * 下一个
     */
    public T next() {
        if (index + 1 >= values.length) {
            index = 0;
            return values[index];
        }
        onChanged();
        return values[index++];
    }

    /**
     * 上一个
     */
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
