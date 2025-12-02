package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectionUtil {
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> matcher) {
        for (T t : collection) {
            if (!matcher.test(t)) return false;
        }

        return true;
    }

    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> matcher) {
        for (T t : collection) {
            if (matcher.test(t)) return true;
        }

        return false;
    }

    public static <K, V, M extends Multimap<K, V>> M newMultimap(M emptyMap, Collection<V> values, Function<V, K> keyFactory) {
        for (V value : values) {
            emptyMap.put(keyFactory.apply(value), value);
        }
        return emptyMap;
    }

    public static <T> LinkedList<T> newLinkedList(int ignored) {
        return new LinkedList<>();
    }

    public static <T> T get(Collection<T> c, int index) {
        Objects.checkIndex(index, c.size());
        int i = 0;
        for (T t : c) {
            if (i == index) return t;
            i++;
        }
        throw new IllegalStateException("Unexpected no value on an in-range-index");
    }

    public static <T> Optional<T> getLast(Collection<T> c) {
        for (Iterator<T> iterator = c.iterator(); iterator.hasNext(); ) {
            T t = iterator.next();
            if (!iterator.hasNext()) return Optional.ofNullable(t);
        }
        return Optional.empty();
    }
}
