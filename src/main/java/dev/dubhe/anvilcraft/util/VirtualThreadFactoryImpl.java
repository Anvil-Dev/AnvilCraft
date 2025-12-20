package dev.dubhe.anvilcraft.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualThreadFactoryImpl implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final String namePrefix;

    public VirtualThreadFactoryImpl() {
        this.namePrefix = "AnvilCraftWorker-" + poolNumber.getAndIncrement();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return Thread.ofVirtual()
            .name(namePrefix)
            .unstarted(runnable);
    }
}
