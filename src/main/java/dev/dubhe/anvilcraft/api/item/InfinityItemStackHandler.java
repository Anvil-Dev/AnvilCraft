package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 创造模式物品处理器：无限供给指定物品。
 * insert 永远返回全部（接受所有物品），
 * extract 永远返回请求量（无限供给已设定的物品）。
 */
public class InfinityItemStackHandler implements ResourceHandler<ItemResource> {
    private ItemStack stack = ItemStack.EMPTY;

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(this.stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.stack.getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        this.stack = resource.toStack(amount);
        return amount;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (this.stack.isEmpty()) return 0;
        ItemResource current = ItemResource.of(this.stack);
        if (!current.equals(resource)) return 0;
        return amount;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack.copy();
    }

    public ItemStack getStack() {
        return this.stack.copy();
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }
}
