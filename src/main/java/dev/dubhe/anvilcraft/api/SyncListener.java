package dev.dubhe.anvilcraft.api;

public interface SyncListener<T> {
    void whenSynced(T value);
}
