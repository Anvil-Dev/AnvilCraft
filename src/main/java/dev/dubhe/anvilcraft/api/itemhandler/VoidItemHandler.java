package dev.dubhe.anvilcraft.api.itemhandler;

import com.google.common.base.Preconditions;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class VoidItemHandler implements ResourceHandler<ItemResource> {
    public static final VoidItemHandler INSTANCE = new VoidItemHandler();

    private VoidItemHandler() {
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        Preconditions.checkElementIndex(index, this.size());
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Preconditions.checkElementIndex(index, this.size());
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        Preconditions.checkElementIndex(index, this.size());
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        Preconditions.checkElementIndex(index, this.size());
        return !resource.has(ModComponents.ETERNAL);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return this.isValid(index, resource) ? amount : 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Preconditions.checkElementIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }
}
