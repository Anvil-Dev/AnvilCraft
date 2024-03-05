package dev.dubhe.anvilcraft.util;

import net.minecraft.core.NonNullList;

public interface INonNullListInjector {
    <E> NonNullList<E> copy();

    static  <E> NonNullList<E> copy(NonNullList<E> list) {
        NonNullList<E> nonNullList = NonNullList.create();
        nonNullList.addAll(list);
        return nonNullList;
    }
}
