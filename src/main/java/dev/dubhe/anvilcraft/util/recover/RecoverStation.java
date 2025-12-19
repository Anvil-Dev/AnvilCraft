package dev.dubhe.anvilcraft.util.recover;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
public class RecoverStation<T> {
    private final EvictingQueue<RecoverEntry<T>> entries;

    public RecoverStation(int maxSize) {
        this.entries = EvictingQueue.create(maxSize);
    }

    private RecoverStation(EvictingQueue<RecoverEntry<T>> queue) {
        this.entries = queue;
    }

    public static <T> RecoverStation<T> create(int maxSize) {
        return new RecoverStation<>(maxSize);
    }

    public Set<UUID> recoverableIds() {
        var ids = ImmutableSet.<UUID>builder();
        for (RecoverEntry<T> entry : this.entries) {
            ids.add(entry.id());
        }
        return ids.build();
    }

    public Optional<RecoverEntry<T>> recover(UUID id) {
        for (Iterator<RecoverEntry<T>> iterator = this.entries.iterator(); iterator.hasNext(); ) {
            RecoverEntry<T> entry = iterator.next();
            if (!entry.id().equals(id)) continue;
            iterator.remove();
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    public void removed(UUID id, T storage) {
        this.entries.offer(new RecoverEntry<>(id, storage));
    }

    public void sync(boolean isClient, Set<UUID> recoverableIds) {
        if (!isClient) return;
        this.entries.clear();
        for (UUID recoverableId : recoverableIds) {
            this.entries.add(new RecoverEntry<>(recoverableId, null));
        }
    }

    public void clear() {
        this.entries.clear();
    }

    public static <T> MapCodec<RecoverStation<T>> codec(MapCodec<T> codec) {
        return CodecUtil.evictingQueueMapCodec(
            RecoverEntry.codec(codec).codec()
        ).xmap(RecoverStation::new, RecoverStation::getEntries);
    }
}