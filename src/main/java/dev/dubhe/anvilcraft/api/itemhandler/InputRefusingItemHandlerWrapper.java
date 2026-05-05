package dev.dubhe.anvilcraft.api.itemhandler;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class InputRefusingItemHandlerWrapper implements ResourceHandler<ItemResource> {
    private final ResourceHandler<ItemResource> delegate;

    public InputRefusingItemHandlerWrapper(ResourceHandler<ItemResource> delegate) {
        this.delegate = delegate;
    }

    public static ResourceHandler<ItemResource> wrap(ResourceHandler<ItemResource> ih) {
        return new InputRefusingItemHandlerWrapper(ih);
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
        return false;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.extract(index, resource, amount, transaction);
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.extract(resource, amount, transaction);
    }
}
