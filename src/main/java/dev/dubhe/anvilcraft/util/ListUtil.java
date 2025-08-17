package dev.dubhe.anvilcraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

public class ListUtil {
    public static <T> Optional<T> safelyGet(List<T> list, int index) {
        if (index < 0 || list.isEmpty() || index >= list.size()) return Optional.empty();
        return Optional.ofNullable(list.get(index));
    }

    public static <T> List<T> cycle(List<T> original, int times) {
        if (times == 0 || original.isEmpty()) return new ArrayList<>(original);
        times %= original.size();
        if (times == 0) return new ArrayList<>(original);
        List<T> cycled = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            cycled.add(original.get((i + times) % original.size()));
        }
        return cycled;
    }

    public static <T> List<T> createWithValues(int size, IntFunction<T> valueFac) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(valueFac.apply(i));
        }
        return result;
    }

    public static <T, R extends T> List<R> cast(List<T> original, Class<R> clazz) {
        ArrayList<R> results = new ArrayList<>();
        for (T t : original) {
            if (clazz.isInstance(t)) {
                results.add(clazz.cast(t));
            }
        }
        return results;
    }

    public static <T, U> boolean equals(List<T> first, List<U> second, BiPredicate<T, U> equals) {
        int size = first.size();
        if (size != second.size()) return false;
        for (int i = 0; i < size; i++) {
            if (!equals.test(first.get(i), second.get(i))) return false;
        }
        return true;
    }
}
