package dev.dubhe.anvilcraft.api.container.recover;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
public class RecoverStation {
    public static final MapCodec<RecoverStation> CODEC = CodecUtil.evictingQueueMapCodec(
        RecoverEntry.CODEC.codec()
    ).xmap(RecoverStation::new, RecoverStation::getEntries);
    public static final StreamCodec<RegistryFriendlyByteBuf, RecoverStation> STREAM_CODEC = CodecUtil.evictingQueueStreamCodec(
        RecoverEntry.STREAM_CODEC
    ).map(RecoverStation::new, RecoverStation::getEntries);
    private final EvictingQueue<RecoverEntry> entries;

    public RecoverStation(int maxSize) {
        this.entries = EvictingQueue.create(maxSize);
    }

    private RecoverStation(EvictingQueue<RecoverEntry> queue) {
        this.entries = queue;
    }

    public static RecoverStation create(int maxSize) {
        return new RecoverStation(maxSize);
    }

    public Set<UUID> recoverableIds() {
        var ids = ImmutableSet.<UUID>builder();
        for (RecoverEntry entry : this.entries) {
            ids.add(entry.id());
        }
        return ids.build();
    }

    public Optional<RecoverEntry> recover(UUID id) {
        for (Iterator<RecoverEntry> iterator = this.entries.iterator(); iterator.hasNext(); ) {
            RecoverEntry entry = iterator.next();
            if (!entry.id().equals(id)) continue;
            iterator.remove();
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    public void removed(UUID id, ContainerStorage storage) {
        this.entries.offer(new RecoverEntry(id, storage));
    }

    public void sync(boolean isClient, Set<UUID> recoverableIds) {
        if (!isClient) return;
        this.entries.clear();
        for (UUID recoverableId : recoverableIds) {
            this.entries.add(new RecoverEntry(recoverableId, null));
        }
    }

    public void clear() {
        this.entries.clear();
    }
}
