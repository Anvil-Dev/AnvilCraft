package dev.dubhe.anvilcraft.api.rendering.foundation.buffer.ring;

import dev.dubhe.anvilcraft.api.rendering.foundation.Disposable;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.BufferHost;

public class RingBuffer<T extends RingBufferElement<C>, C extends BufferHost> implements Disposable {
    public static final int DEFAULT_SIZE = 5;

    private final int size;
    private int currentUsing = 0;
    private final C context;
    private final RingBufferElement.Factory<T, C> factory;
    private final RingBufferElement<C>[] elements;

    public RingBuffer(int size, C context, RingBufferElement.Factory<T, C> factory) {
        this.size = size;
        this.context = context;
        this.factory = factory;
        elements = new RingBufferElement[size];
    }

    public RingBuffer(C context, RingBufferElement.Factory<T, C> factory) {
        this(DEFAULT_SIZE, context, factory);
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (currentUsing < size - 1) {
            T element = (T) elements[currentUsing++];
            if (element == null) {
                elements[currentUsing] = element = factory.create(context);
            }
            element.waitForReady();
            element.setup();
            return element;
        }
        currentUsing = 0;
        T element = (T) elements[0];
        if (element == null) {
            elements[currentUsing] = element = factory.create(context);
        }
        element.waitForReady();
        element.setup();
        return element;
    }

    @SuppressWarnings("unchecked")
    public T current() {
        T element = (T) elements[currentUsing];
        if (element == null) {
            elements[currentUsing] = element = factory.create(context);
        }
        return element;
    }


    @Override
    public void dispose() {
        for (RingBufferElement<C> element : elements) {
            if (element != null) {
                element.dispose();
            }
        }
    }
}
