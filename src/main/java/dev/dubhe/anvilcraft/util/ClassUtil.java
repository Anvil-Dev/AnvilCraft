package dev.dubhe.anvilcraft.util;

import org.jetbrains.annotations.Nullable;

public class ClassUtil {
    /**
     * 如果该加载器已被 Java 虚拟机记录为该二进制名称类的启动加载器，则返回带有该二进制名称的类。否则返回 {@code null} 。
     *
     * @param className 需要获取的类名
     * @return {@code Class} 对象。如果该类尚未加载，则为 {@code null}
     * @see ClassLoader#loadClass(String)
     */
    public static @Nullable Class<?> getLoadedClass(String className) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> clazz = ClassUtil.findLoadedClass(loader, className);

        if (clazz == null) {
            loader = ClassLoader.getSystemClassLoader();
            clazz = ClassUtil.findLoadedClass(loader, className);
        }

        if (clazz == null) {
            loader = ClassUtil.class.getClassLoader();
            clazz = ClassUtil.findLoadedClass(loader, className);
        }

        return clazz;
    }

    private static @Nullable Class<?> findLoadedClass(@Nullable ClassLoader loader, String className) throws ClassNotFoundException {
        if (loader == null) {
            return null;
        }

        return loader.loadClass(className);
    }
}
