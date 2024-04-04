package dev.dubhe.anvilcraft.util;

import net.minecraft.core.NonNullList;
import org.jetbrains.annotations.NotNull;

public interface NonNullListUtils {
    static <E> @NotNull NonNullList<E> copy(NonNullList<E> list) {
        NonNullList<E> nonNullList = NonNullList.create();
        nonNullList.addAll(list);
        return nonNullList;
    }
}
