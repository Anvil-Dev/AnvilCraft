package dev.dubhe.anvilcraft.util.recover;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableSet;
import dev.anvilcraft.lib.v2.util.nullness.NullableType;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public CompoundTag serializeNBT(Function<@NullableType T, Tag> encoder) {
        ListTag list = new ListTag();
        for (RecoverEntry<T> entry : this.entries) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", entry.id());
            tag.put("value", encoder.apply(entry.value()));
            list.add(tag);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("entries", list);
        return tag;
    }

    public void deserializeNBT(BiFunction<UUID, CompoundTag, T> decoder, CompoundTag tag) {
        this.entries.clear();

        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            UUID id = entryTag.getUUID("id");
            if (entryTag.contains("value", Tag.TAG_COMPOUND)) {
                T value = decoder.apply(id, entryTag.getCompound("value"));
                if (value != null) {
                    this.entries.add(new RecoverEntry<>(id, value));
                }
            } else {
                this.entries.add(new RecoverEntry<>(id, null));
            }
        }
    }
}
