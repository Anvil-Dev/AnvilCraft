package dev.dubhe.anvilcraft.util;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ListUtil {
    public static <T> @Nullable T safelyGet(List<T> list, int index) {
        if (index < 0 || list.isEmpty() || index >= list.size()) return null;
        return list.get(index);
    }
}
