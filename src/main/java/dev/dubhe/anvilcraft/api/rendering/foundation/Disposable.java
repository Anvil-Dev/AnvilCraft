package dev.dubhe.anvilcraft.api.rendering.foundation;

public interface Disposable {
    /**
     * It is guaranteed to run on Render Thread.
     */
    void dispose();
}
