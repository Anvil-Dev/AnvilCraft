package dev.dubhe.anvilcraft.util;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryImpl implements java.util.concurrent.ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    @SuppressWarnings("removal")
    public ThreadFactoryImpl() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null)
            ? s.getThreadGroup()
            : Thread.currentThread().getThreadGroup();
        namePrefix = "AnvilCraftWorker-" + poolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(
            group,
            r,
            namePrefix + threadNumber.getAndIncrement(),
            0
        );
        t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
