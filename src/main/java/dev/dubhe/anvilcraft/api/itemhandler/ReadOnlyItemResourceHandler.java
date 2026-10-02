package dev.dubhe.anvilcraft.api.itemhandler;

import com.google.common.base.Preconditions;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

public final class ReadOnlyItemResourceHandler implements ResourceHandler<ItemResource> {
    private final ResourceHandler<ItemResource> delegate;

    public ReadOnlyItemResourceHandler(ResourceHandler<ItemResource> delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return this.delegate.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.delegate.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return this.delegate.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        Preconditions.checkElementIndex(index, this.size());
        return false;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Preconditions.checkElementIndex(index, this.size());
        return this.insert(resource, amount, transaction);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Preconditions.checkElementIndex(index, this.size());
        return this.extract(resource, amount, transaction);
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    public int insertBypass(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.insert(index, resource, amount, transaction);
    }

    public int extractBypass(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.extract(index, resource, amount, transaction);
    }
}
