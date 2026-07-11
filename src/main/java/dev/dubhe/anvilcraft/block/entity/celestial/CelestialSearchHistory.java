package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 有容量上限的天体搜索历史，同时维护服务端浏览状态。
 */
public final class CelestialSearchHistory {
    private static final int MAX_ENTRIES = 10;

    private final List<Entry> entries = new ArrayList<>();
    private int browseIndex;
    private @Nullable Entry originalEntry;

    /** 添加结果；若与最新记录完全相同则忽略。 */
    public void add(CelestialBodyData body, @Nullable PlanetaryResourceSet resources) {
        if (!this.entries.isEmpty() && this.entries.getFirst().body().toTag().equals(body.toTag())) {
            return;
        }
        this.entries.addFirst(new Entry(body, resources));
        while (this.entries.size() > MAX_ENTRIES) {
            this.entries.removeLast();
        }
        this.resetBrowsing();
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public void clear() {
        this.entries.clear();
        this.resetBrowsing();
    }

    public boolean hasPrevious() {
        return this.entries.size() > 1 && this.browseIndex < this.entries.size();
    }

    public boolean hasNext() {
        return this.browseIndex > 0;
    }

    /** 浏览更早的结果；最新记录代表当前天体，因此首次后退会跳过它。 */
    public @Nullable Entry previous(
        @Nullable CelestialBodyData currentBody,
        @Nullable PlanetaryResourceSet currentResources
    ) {
        int size = this.entries.size();
        if (size <= 1 || this.browseIndex >= size || currentBody == null) return null;
        if (this.browseIndex == 0) {
            this.originalEntry = new Entry(currentBody, currentResources);
            this.browseIndex = 1;
        }
        this.browseIndex++;
        if (this.browseIndex > size) return null;
        return this.entries.get(this.browseIndex - 1);
    }

    /** 向开始浏览时保存的原始结果方向前进。 */
    public @Nullable Entry next() {
        if (this.browseIndex <= 0) return null;
        this.browseIndex--;
        if (this.browseIndex == 0) {
            Entry result = this.originalEntry != null
                ? this.originalEntry
                : this.entries.isEmpty() ? null : this.entries.getFirst();
            this.originalEntry = null;
            return result;
        }
        return this.entries.get(this.browseIndex - 1);
    }

    public int browseIndex() {
        return this.browseIndex;
    }

    public void setBrowseIndex(int browseIndex) {
        this.browseIndex = Math.clamp(browseIndex, 0, this.entries.size());
        this.originalEntry = null;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("size", Math.min(this.entries.size(), MAX_ENTRIES));
        for (int i = 0; i < Math.min(this.entries.size(), MAX_ENTRIES); i++) {
            tag.put("h" + i, this.entries.get(i).toTag());
        }
        return tag;
    }

    /** 同时兼容当前“天体加资源”格式和旧版仅保存天体的格式。 */
    public void load(CompoundTag tag) {
        this.clear();
        int size = Math.min(tag.getIntOr("size", 0), MAX_ENTRIES);
        for (int i = 0; i < size; i++) {
            String key = "h" + i;
            if (!tag.contains(key)) continue;
            CompoundTag entryTag = tag.getCompoundOrEmpty(key);
            if (entryTag.contains("body")) {
                this.entries.add(Entry.fromTag(entryTag));
            } else {
                this.entries.add(new Entry(CelestialBodyData.fromTag(entryTag), null));
            }
        }
    }

    private void resetBrowsing() {
        this.browseIndex = 0;
        this.originalEntry = null;
    }

    /** 一次搜索得到的天体及其同步生成的资源集合。 */
    public record Entry(CelestialBodyData body, @Nullable PlanetaryResourceSet resources) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("body", this.body.toTag());
            if (this.resources != null) {
                tag.put("resources", this.resources.toTag());
            }
            return tag;
        }

        public static Entry fromTag(CompoundTag tag) {
            CelestialBodyData body = CelestialBodyData.fromTag(tag.getCompoundOrEmpty("body"));
            PlanetaryResourceSet resources = tag.contains("resources")
                ? PlanetaryResourceSet.fromTag(tag.getCompoundOrEmpty("resources"))
                : null;
            return new Entry(body, resources);
        }
    }
}
