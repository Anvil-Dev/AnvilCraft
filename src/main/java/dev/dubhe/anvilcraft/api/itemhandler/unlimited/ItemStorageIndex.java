package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

/** 随单槽变更维护种类、数量与空槽，查询仅访问指定物品的条目。 */
final class ItemStorageIndex<K> {
    private final Map<K, NavigableSet<Integer>> positions = new HashMap<>();
    private final Map<K, Long> counts = new HashMap<>();
    private final Map<Integer, Entry<K>> entries = new HashMap<>();
    private final NavigableSet<Integer> empty = new TreeSet<>();
    private long space;
    private int size;

    void clear() {
        this.positions.clear();
        this.counts.clear();
        this.entries.clear();
        this.empty.clear();
        this.space = 0;
        this.size = 0;
    }

    void update(int slot, @Nullable K key, int amount, int maxStackSize) {
        while (this.size <= slot) {
            this.empty.add(this.size++);
        }
        Entry<K> old = this.entries.remove(slot);
        if (old != null) {
            NavigableSet<Integer> slots = this.positions.get(old.key());
            slots.remove(slot);
            long remaining = this.counts.get(old.key()) - old.amount();
            if (slots.isEmpty()) {
                this.positions.remove(old.key());
                this.counts.remove(old.key());
            } else {
                this.counts.put(old.key(), remaining);
            }
            this.space -= old.space();
        }
        if (key == null || amount == 0) {
            this.empty.add(slot);
            return;
        }
        long used = (long) Math.ceilDiv(64, maxStackSize) * amount;
        this.entries.put(slot, new Entry<>(key, amount, used));
        this.empty.remove(slot);
        this.positions.computeIfAbsent(key, ignored -> new TreeSet<>()).add(slot);
        this.counts.merge(key, (long) amount, Long::sum);
        this.space += used;
    }

    int first(K key) {
        NavigableSet<Integer> slots = this.positions.get(key);
        return slots == null ? -1 : slots.first();
    }

    List<Integer> slots(K key) {
        NavigableSet<Integer> slots = this.positions.get(key);
        return slots == null ? List.of() : List.copyOf(slots);
    }

    long count(K key) {
        return this.counts.getOrDefault(key, 0L);
    }

    int emptySlot() {
        return this.empty.isEmpty() ? this.size : this.empty.first();
    }

    int entries() {
        return this.entries.size();
    }

    int types() {
        return this.positions.size();
    }

    int space() {
        return (int) Math.min(Integer.MAX_VALUE, this.space);
    }

    private record Entry<K>(K key, int amount, long space) {
    }
}
