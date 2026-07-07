package dev.dubhe.anvilcraft.util;

import lombok.Getter;

@Getter
public class MutableValue<T> {
    private T value;

    public MutableValue(T value) {
        this.value = value;
    }

    public void setValue(T value) {
        if (!this.value.equals(value)) {
            this.value = value;
        }
    }
}
