package dev.dubhe.anvilcraft.saved.sc.server;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.SyncListener;
import dev.dubhe.anvilcraft.api.sc.category.store.Categories;
import dev.dubhe.anvilcraft.api.sc.item.ItemEntries;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrades;
import dev.dubhe.anvilcraft.saved.sc.SCStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * 服务端的潜影集装箱存储。
 */
@Getter
public class ServerSCStorage extends SCStorage {
    public static final MapCodec<ServerSCStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("id")
            .forGetter(ServerSCStorage::getId),
        ItemEntries.CODEC
            .forGetter(ServerSCStorage::getEntries),
        Categories.CODEC
            .forGetter(ServerSCStorage::getCategories),
        Upgrades.CODEC.codec()
            .optionalFieldOf("upgrades", new Upgrades())
            .forGetter(ServerSCStorage::getUpgrades)
    ).apply(ins, ServerSCStorage::new));
    private final Categories categories;
    private final Int2ObjectSortedMap<SyncListener<ServerSCStorage>> syncListeners = new Int2ObjectLinkedOpenHashMap<>();

    public ServerSCStorage(UUID id) {
        super(id);
        this.categories = Categories.create();
    }

    private ServerSCStorage(UUID id, ItemEntries entries, Categories categories, Upgrades upgrades) {
        super(id, entries, upgrades);
        this.categories = categories;
    }

    @Override
    public ItemStack splitUnchecked(int index, int amount) {
        this.markDirty();
        return super.splitUnchecked(index, amount);
    }

    public void sync(ServerSCStorage storage) {
        this.upgrades.sync(storage.upgrades);
        this.entries.sync(storage.entries);
        this.categories.sync(storage.categories);
        this.markDirty();
    }

    public void applyCategory(ServerSCStorage source) {
        this.categories.sync(source.categories);
        for (SyncListener<ServerSCStorage> listener : this.syncListeners.values()) {
            listener.whenSynced(this);
        }
        this.markDirty();
    }

    private void markDirty() {
        ServerSCStorages.get().setDirty();
    }

    public int addSyncListener(SyncListener<ServerSCStorage> listener) {
        int key = this.syncListeners.isEmpty() ? 0 : this.syncListeners.lastIntKey() + 1;
        this.syncListeners.put(key, listener);
        return key;
    }

    public void removeSyncListener(int index) {
        this.syncListeners.remove(index);
    }

    @Override
    public ServerSCStorage intoServer() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ServerSCStorage storage)) return false;
        return Objects.equals(this.getId(), storage.getId())
               && Objects.equals(this.getEntries(), storage.getEntries())
               && Objects.equals(this.getCategories(), storage.getCategories())
               && Objects.equals(this.getUpgrades(), storage.getUpgrades());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getEntries(), this.getCategories(), this.getUpgrades());
    }
}
