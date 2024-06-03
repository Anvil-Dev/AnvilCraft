package dev.dubhe.anvilcraft.util;

/**
 * 状态监听器
 */
public interface StateListener<T> {

    T getState();

    void notifyStateChanged(T newState);
}
