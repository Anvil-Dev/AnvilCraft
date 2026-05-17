package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.network.CyclingValueSyncPacket;
import lombok.Getter;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

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
        this.onChangedCallback.accept(this);
    }

    public int count() {
        return this.values.length;
    }

    /**
     * 设置当前index
     *
     * @return this
     */
    public WatchableCyclingValue<T> fromIndex(int index) {
        if (index >= this.values.length) {
            throw new IndexOutOfBoundsException(index);
        }
        this.index = index;
        this.onChanged();
        return this;
    }

    public int index() {
        return this.index;
    }

    public T get() {
        return this.values[this.index];
    }

    /**
     * 下一个
     */
    public T next() {
        if (this.index + 1 >= this.values.length) {
            this.index = 0;
            return this.values[this.index];
        }
        this.onChanged();
        return this.values[this.index++];
    }

    /**
     * 上一个
     */
    public T previous() {
        if (this.index - 1 < 0) {
            this.index = this.values.length - 1;
            return this.values[this.index];
        }
        this.onChanged();
        return this.values[this.index--];
    }

    public void notifyServer() {
        ClientPacketDistributor.sendToServer(new CyclingValueSyncPacket(this.index, this.name));
    }
}
