package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * 真正无限容量的物品处理器。
 *
 * <p>既不限物品种类数量，也不限每种物品的存储数量。存储按需动态增长，不预先分配槽位；
 * 当某物品条目的数量达到 {@link Integer#MAX_VALUE} 后，会追加同种物品的新条目继续累加，
 * 因此每种物品的总量不受 {@code int} 上限约束，仅受列表长度的现实约束。</p>
 */
public class InfiniteItemStacksResourceHandler extends UnlimitedItemStacksResourceHandler {

    public InfiniteItemStacksResourceHandler() {
        super(0);
    }

    /**
     * 同一物品可能占用多个条目，故按「不同物品」去重统计种类数。
     */
    @Override
    public int getTypeCount() {
        Set<ResourceKey> types = new HashSet<>();
        for (UnlimitedItemStack stack : this.stacks) {
            if (stack.isEmpty()) continue;
            types.add(ResourceKey.of(stack.toStack()));
        }
        return types.size();
    }

    /**
     * 槽位数为「条目数 + 1」，多出的一个作为新条目增长槽。
     *
     * <p>与基类不同，这里基于条目数而非种类数：同一物品可能占用多个条目，若按种类数暴露槽位，
     * 消费者将无法触及同一物品的后续条目。由于本处理器的插入永不失败（总是追加新条目并吞下物品），
     * 不会出现 {@code ItemHandlerHelper} 遍历循环因槽位增长而无限循环的问题。</p>
     */
    @Override
    public int getSlots() {
        return (int) Math.min((long) this.stacks.size() + 1, Integer.MAX_VALUE);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot >= this.stacks.size()) {
            if (slot >= this.getSlots()) {
                return stack;
            }
            if (simulate) {
                return ItemStack.EMPTY;
            }
            this.ensureSlot(slot);
        }
        this.validateSlotIndex(slot);

        UnlimitedItemStack existing = this.stacks.get(slot);
        if (!existing.isEmpty() && !existing.isSameItemSameComponents(stack)) {
            return stack;
        }
        // 用 long 计算剩余空间，避免 existing 数量接近 int 上限时 grow 溢出为负数
        int amount = (int) Math.min(stack.getCount(), (long) Integer.MAX_VALUE - existing.getCount());
        if (amount <= 0) {
            return stack;
        }
        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, new UnlimitedItemStack(stack.copyWithCount(amount)));
            } else {
                existing.grow(amount);
            }
            this.onContentsChanged(slot, existing);
        }
        ItemStack leftover = stack.copy();
        leftover.shrink(amount);
        return leftover;
    }

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int slot = 0; slot < this.size() && !stack.isEmpty(); slot++) {
            UnlimitedItemStack existing = this.stacks.get(slot);
            if (existing.isEmpty() || existing.isSameItemSameComponents(stack)) {
                stack = this.insertItem(slot, stack, simulate);
            }
        }
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (simulate) {
            return ItemStack.EMPTY;
        }
        this.ensureSlot(this.size());
        return this.insertItem(this.size() - 1, stack, false);
    }

    /**
     * 无限处理器采用稀疏结构（初始 0 条目），同步时直接替换为源的全部非空条目，
     * 否则按固定槽位复制会丢弃超出的物品。
     */
    @Override
    public void sync(UnlimitedItemStacksResourceHandler items) {
        this.setStacks(UnlimitedItemStacksResourceHandler.trim(items.copyToList()));
    }

    @Override
    protected void setStacks(NonNullList<UnlimitedItemStack> stacks) {
        this.stacks.clear();
        this.stacks.addAll(stacks);
        this.onContentsChanged(-1, UnlimitedItemStack.EMPTY);
    }
}
