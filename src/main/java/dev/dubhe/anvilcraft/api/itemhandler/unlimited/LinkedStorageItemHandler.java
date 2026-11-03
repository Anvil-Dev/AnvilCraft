package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.UUID;

/**
 * 大仓库的物品访问接口，保留端口库存的独立归属，并在核心库存耗尽后取用端口库存。
 */
public final class LinkedStorageItemHandler extends UnlimitedItemStacksResourceHandler {
    private final UUID storageId;
    private final UnlimitedItemStacksResourceHandler core;
    private List<StoragePortBlockEntity> ports = List.of();
    private long portVersion = -1;

    public LinkedStorageItemHandler(UUID storageId, UnlimitedItemStacksResourceHandler core) {
        super(0);
        this.storageId = storageId;
        this.core = core;
    }

    private List<StoragePortBlockEntity> ports() {
        long version = StoragePortManager.itemPortVersion();
        if (this.portVersion != version) {
            this.ports = StoragePortManager.liveItemPorts(this.storageId);
            this.portVersion = version;
        }
        return this.ports;
    }

    @Override
    public int size() {
        return this.core.size() + 1 + this.ports().size() * StoragePortBlockEntity.BUFFER_SLOTS;
    }

    @Override
    public ItemResource getResource(int index) {
        if (index < 0) {
            return ItemResource.EMPTY;
        }
        if (index < this.core.size()) {
            return this.core.getResource(index);
        }
        if (index == this.core.size()) {
            return ItemResource.EMPTY;
        }
        int offset = index - this.core.size() - 1;
        List<StoragePortBlockEntity> linked = this.ports();
        int portIndex = offset / StoragePortBlockEntity.BUFFER_SLOTS;
        if (portIndex >= linked.size() || !linked.get(portIndex).isConnectedTo(this.storageId)) {
            return ItemResource.EMPTY;
        }
        return linked.get(portIndex).getBuffer().getResource(offset % StoragePortBlockEntity.BUFFER_SLOTS);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index < 0) {
            return 0;
        }
        if (index < this.core.size()) {
            return this.core.getAmountAsLong(index);
        }
        if (index == this.core.size()) {
            return 0;
        }
        int offset = index - this.core.size() - 1;
        List<StoragePortBlockEntity> linked = this.ports();
        int portIndex = offset / StoragePortBlockEntity.BUFFER_SLOTS;
        if (portIndex >= linked.size() || !linked.get(portIndex).isConnectedTo(this.storageId)) {
            return 0;
        }
        return linked.get(portIndex).getBuffer().getAmountAsLong(offset % StoragePortBlockEntity.BUFFER_SLOTS);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (index == this.core.size()) {
            return this.core.isValid(0, resource) ? Integer.MAX_VALUE : 0;
        }
        return index >= 0 && index < this.core.size() ? this.core.getCapacityAsLong(index, resource) : 0;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return index >= 0 && index <= this.core.size() && this.core.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index == this.core.size()) {
            return this.core.insert(resource, amount, transaction);
        }
        return index >= 0 && index < this.core.size() ? this.core.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        return this.core.insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!this.getResource(index).equals(resource)) {
            return 0;
        }
        int requested = (int) Math.min(amount, this.getAmountAsLong(index));
        if (index < this.core.size()) {
            return this.core.extract(index, resource, requested, transaction);
        }
        return this.extract(resource, requested, transaction);
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int extracted = this.core.extract(resource, amount, transaction);
        for (StoragePortBlockEntity port : this.ports()) {
            if (extracted == amount) {
                break;
            }
            if (port.isConnectedTo(this.storageId)) {
                extracted += port.getBuffer().extract(resource, amount - extracted, transaction);
            }
        }
        return extracted;
    }

    @Override
    public int getTypeCount() {
        return this.core.getTypeCount();
    }

    @Override
    public int getTypeLimit() {
        return this.core.getTypeLimit();
    }

    @Override
    public double getFullness() {
        return this.core.getFullness();
    }
}
