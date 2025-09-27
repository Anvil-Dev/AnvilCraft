package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.ring;

import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.BufferHost;

public interface RingBufferElement<C extends BufferHost> extends Disposable {

    void waitForReady();

    void setup();

    public interface Factory<T extends RingBufferElement<C>, C extends BufferHost> {
        T create(C context);
    }
}
