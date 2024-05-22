package dev.dubhe.anvilcraft.util;

public interface StateListener<T> {

    T getState();

    void notifyStateChanged(T newState);
}
