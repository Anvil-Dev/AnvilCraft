package dev.dubhe.anvilcraft.saved.sc.client;

import dev.dubhe.anvilcraft.api.SyncListener;
import dev.dubhe.anvilcraft.api.sc.category.store.Categories;
import dev.dubhe.anvilcraft.api.sc.category.store.ClientCategories;
import dev.dubhe.anvilcraft.api.sc.item.ItemEntries;
import dev.dubhe.anvilcraft.api.sc.item.OrderPos;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrades;
import dev.dubhe.anvilcraft.saved.sc.SCStorage;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 客户端的潜影集装箱存储。<br>
 * 除了修改名称或类别模式和增删同步监听器外，仅提供查询。<br>
 * 注意：修改其它部分将可能导致同步问题！
 */
@Getter
public class ClientSCStorage extends SCStorage {
    private final ClientCategories categories = ClientCategories.create();
    private final Int2ObjectSortedMap<SyncListener<ClientSCStorage>> syncListeners = new Int2ObjectLinkedOpenHashMap<>();

    public ClientSCStorage(UUID id) {
        super(id);
    }

    public @Unmodifiable List<OrderPos> getOrder(
        Predicate<UnlimitedItemStack> filter,
        Comparator<UnlimitedItemStack> sorter,
        boolean shouldFold
    ) {
        return this.entries.getOrder(
            filter.and(this.getCategories().getEnabled(Minecraft.getInstance().level.registryAccess())::test),
            sorter,
            shouldFold
        );
    }

    public void sync(ItemEntries entries) {
        this.entries.sync(entries);
        for (SyncListener<ClientSCStorage> listener : this.syncListeners.values()) {
            listener.whenSynced(this);
        }
    }

    public void sync(Categories categories) {
        this.categories.sync(categories);
        for (SyncListener<ClientSCStorage> listener : this.syncListeners.values()) {
            listener.whenSynced(this);
        }
    }

    public void sync(Upgrades upgrades) {
        this.upgrades.sync(upgrades);
        for (SyncListener<ClientSCStorage> listener : this.syncListeners.values()) {
            listener.whenSynced(this);
        }
    }

    public int addSyncListener(SyncListener<ClientSCStorage> listener) {
        int key = this.syncListeners.isEmpty() ? 0 : this.syncListeners.lastIntKey() + 1;
        this.syncListeners.put(key, listener);
        return key;
    }

    public void removeSyncListener(int index) {
        this.syncListeners.remove(index);
    }

    @Override
    public ClientSCStorage intoClient() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientSCStorage storage)) return false;
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
