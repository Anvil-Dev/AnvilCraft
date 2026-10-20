package dev.dubhe.anvilcraft.client.support;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

public final class StorageCraftingPrediction {
    private static final int MAX_PENDING = 64;
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    private ItemStack preview = ItemStack.EMPTY;
    private boolean dirty = true;

    public boolean add(ItemStack carried, ItemStack result) {
        if (result.isEmpty() || this.pending.size() >= MAX_PENDING) return false;
        ItemStack predicted = this.carried(carried);
        if (!predicted.isEmpty() && (!ItemStack.isSameItemSameComponents(predicted, result)
            || predicted.getCount() + result.getCount() > result.getMaxStackSize())) {
            return false;
        }
        this.pending.addLast(result.copy());
        this.dirty = true;
        return true;
    }

    public void acknowledge() {
        this.pending.removeFirst();
        this.dirty = true;
    }

    public boolean isPending() {
        return !this.pending.isEmpty();
    }

    public ItemStack carried(ItemStack confirmed) {
        if (this.pending.isEmpty()) return confirmed;
        if (!this.dirty) return this.preview;
        ItemStack predicted = confirmed.copy();
        for (ItemStack result : this.pending) {
            if (predicted.isEmpty()) {
                predicted = result.copy();
            } else if (ItemStack.isSameItemSameComponents(predicted, result)) {
                predicted.setCount(Math.min(predicted.getMaxStackSize(), predicted.getCount() + result.getCount()));
            }
        }
        this.preview = predicted;
        this.dirty = false;
        return this.preview;
    }
}
