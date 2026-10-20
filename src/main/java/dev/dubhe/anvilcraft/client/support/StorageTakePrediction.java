package dev.dubhe.anvilcraft.client.support;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.world.item.ItemStack;

/** 只预测光标和可见数量；背包与实际库存始终由服务端同步。 */
public final class StorageTakePrediction {
    private static final int MAX_PENDING = 64;
    private final Int2LongMap taken = new Int2LongOpenHashMap();
    private final boolean quickMove;
    private ItemStack carried = ItemStack.EMPTY;
    private int pending;

    public StorageTakePrediction(boolean quickMove) {
        this.quickMove = quickMove;
    }

    public boolean add(int slot, ItemStack item, long available, int button, boolean quickMove) {
        if (this.quickMove != quickMove || this.pending >= MAX_PENDING || item.isEmpty()
            || !quickMove && !this.taken.isEmpty()
            || !this.taken.containsKey(slot) && this.taken.size() >= MAX_PENDING) return false;
        int amount = (int) Math.min(item.getMaxStackSize(), Math.max(0, available - this.taken.get(slot)));
        if (!quickMove && button == 1) amount = Math.ceilDiv(amount, 2);
        if (amount == 0) return false;
        this.taken.mergeLong(slot, amount, Long::sum);
        if (!quickMove) this.carried = item.copyWithCount(amount);
        this.pending++;
        return true;
    }

    public void acknowledge() {
        this.pending--;
    }

    public boolean isPending() {
        return this.pending > 0;
    }

    public long taken(int slot) {
        return this.taken.get(slot);
    }

    public ItemStack carried(ItemStack confirmed) {
        return this.isPending() && !this.quickMove ? this.carried : confirmed;
    }
}
