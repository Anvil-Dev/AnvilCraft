package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Maintains the bounded CFA search history and its browse cursor. */
public final class CelestialSearchHistory {
    private static final int MAX_ENTRIES = 10;

    private final List<CelestialForgingAnvilBlockEntity.SearchHistoryEntry> entries;
    private int browseIndex;
    private @Nullable CelestialForgingAnvilBlockEntity.SearchHistoryEntry originalEntry;

    public CelestialSearchHistory(List<CelestialForgingAnvilBlockEntity.SearchHistoryEntry> entries) {
        this.entries = Objects.requireNonNull(entries);
    }

    public List<CelestialForgingAnvilBlockEntity.SearchHistoryEntry> entries() {
        return this.entries;
    }

    public void clear() {
        this.entries.clear();
        this.resetBrowsing();
    }

    public void add(
        CelestialBodyData body,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (!this.entries.isEmpty()
            && this.entries.getFirst().body().toTag().equals(body.toTag())) {
            return;
        }
        this.entries.addFirst(new CelestialForgingAnvilBlockEntity.SearchHistoryEntry(body, resources));
        while (this.entries.size() > MAX_ENTRIES) this.entries.removeLast();
        this.resetBrowsing();
    }

    public boolean hasPrevious() {
        return this.entries.size() > 1 && this.browseIndex < this.entries.size();
    }

    public boolean hasNext() {
        return this.browseIndex > 0;
    }

    public int browseIndex() {
        return this.browseIndex;
    }

    public void setBrowseIndex(int browseIndex) {
        this.browseIndex = Math.clamp(browseIndex, 0, this.entries.size());
        this.originalEntry = null;
    }

    public @Nullable CelestialForgingAnvilBlockEntity.SearchHistoryEntry previous(
        @Nullable CelestialBodyData currentBody,
        @Nullable PlanetaryResourceSet currentResources
    ) {
        int size = this.entries.size();
        if (size <= 1 || this.browseIndex >= size || currentBody == null) return null;
        if (this.browseIndex == 0) {
            this.originalEntry = new CelestialForgingAnvilBlockEntity.SearchHistoryEntry(
                currentBody,
                currentResources
            );
            this.browseIndex = 1;
        }
        this.browseIndex++;
        if (this.browseIndex > size) return null;
        return this.entries.get(this.browseIndex - 1);
    }

    public @Nullable CelestialForgingAnvilBlockEntity.SearchHistoryEntry next() {
        if (this.browseIndex <= 0) return null;
        this.browseIndex--;
        if (this.browseIndex == 0) {
            CelestialForgingAnvilBlockEntity.SearchHistoryEntry result = this.originalEntry;
            if (result == null && !this.entries.isEmpty()) result = this.entries.getFirst();
            this.originalEntry = null;
            return result;
        }
        return this.entries.get(this.browseIndex - 1);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        int size = Math.min(this.entries.size(), MAX_ENTRIES);
        tag.putInt("size", size);
        for (int index = 0; index < size; index++) {
            tag.put("h" + index, this.entries.get(index).toTag());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        this.clear();
        int size = Math.min(Math.max(tag.getInt("size"), 0), MAX_ENTRIES);
        for (int index = 0; index < size; index++) {
            String key = "h" + index;
            if (!tag.contains(key)) continue;
            CompoundTag entryTag = tag.getCompound(key);
            if (entryTag.contains("body")) {
                this.entries.add(CelestialForgingAnvilBlockEntity.SearchHistoryEntry.fromTag(entryTag));
            } else {
                this.entries.add(new CelestialForgingAnvilBlockEntity.SearchHistoryEntry(
                    CelestialBodyData.fromTag(entryTag),
                    null
                ));
            }
        }
    }

    private void resetBrowsing() {
        this.browseIndex = 0;
        this.originalEntry = null;
    }
}
