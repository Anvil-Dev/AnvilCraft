package dev.dubhe.anvilcraft.util;

public class CyclingValue<T> {
    public final T[] values;
    private int index = 0;

    public CyclingValue(T... values) {
        this.values = values;
    }

    public int count() {
        return values.length;
    }

    public CyclingValue<T> fromIndex(int index) {
        if (index >= values.length) {
            throw new IndexOutOfBoundsException(index);
        }
        this.index = index;
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
        return values[index++];
    }

    public T previous() {
        if (index - 1 < 0) {
            index = values.length - 1;
            return values[index];
        }
        return values[index--];
    }
}
