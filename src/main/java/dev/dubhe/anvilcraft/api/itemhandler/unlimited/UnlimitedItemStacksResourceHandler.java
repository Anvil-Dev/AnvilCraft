package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class UnlimitedItemStacksResourceHandler implements IItemHandler, INBTSerializable<CompoundTag> {
    public static final String STACKS_KEY = "stacks";
    protected final NonNullList<UnlimitedItemStack> stacks;

    public UnlimitedItemStacksResourceHandler(int size) {
        this.stacks = NonNullList.create();
        for (int index = 0; index < size; index++) {
            this.stacks.add(UnlimitedItemStack.EMPTY);
        }
    }

    public UnlimitedItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks) {
        this.stacks = NonNullList.create();
        this.stacks.addAll(stacks);
    }

    public int size() {
        return this.stacks.size();
    }

    /**
     * 暴露给外部 {@code IItemHandler} 消费者的槽位数。
     *
     * <p>存储处理器只保留非空类型槽位（稀疏设计），但漏斗/管道等 {@code IItemHandler}
     * 消费者需要至少一个可插入/可追加的槽位。这里返回「现有非空类型数 + 1」的槽位，
     * 多出的一个作为「新类型」的增长槽。</p>
     *
     * <p>注意：必须基于 {@link #getTypeCount()}（非空类型数）而非 {@link #size()}（列表长度），
     * 否则在增长槽插入失败（空间/类型满）时会无限扩大槽位数，使
     * {@code ItemHandlerHelper.insertItemStacked} 的遍历循环永不终止。</p>
     */
    @Override
    public int getSlots() {
        return Math.min(this.getTypeCount() + 1, this.getTypeLimit());
    }

    public UnlimitedItemStack getUnlimitedStackInSlot(int index) {
        return this.stacks.get(index);
    }

    public long getAmountAsLong(int index) {
        return this.stacks.get(index).getCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= this.stacks.size()) {
            return ItemStack.EMPTY;
        }
        return this.stacks.get(slot).toStack();
    }

    /**
     * 把存储扩展到至少包含 {@code slot} 槽位（追加空槽）。
     */
    protected void ensureSlot(int slot) {
        while (this.stacks.size() <= slot) {
            this.stacks.add(UnlimitedItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.isItemValid(slot, stack)) return stack;
        if (slot >= this.stacks.size()) {
            if (slot >= this.getSlots()) {
                return stack;
            }
            if (simulate) {
                // 增长槽：无容量上限的基类接受全部
                return ItemStack.EMPTY;
            }
            this.ensureSlot(slot);
        }
        this.validateSlotIndex(slot);

        UnlimitedItemStack existing = this.stacks.get(slot);
        if (!existing.isEmpty() && !existing.isSameItemSameComponents(stack)) {
            return stack;
        }
        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, new UnlimitedItemStack(stack));
            } else {
                existing.grow(stack.getCount());
            }
            this.onContentsChanged(slot, existing);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        if (slot < 0 || slot >= this.stacks.size()) {
            return ItemStack.EMPTY;
        }

        UnlimitedItemStack existing = this.stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getStack().getMaxStackSize());
        toExtract = Math.min(toExtract, existing.getCount());
        if (toExtract <= 0) return ItemStack.EMPTY;

        ItemStack result = existing.copyWithCount(toExtract).toStack();
        if (!simulate) {
            if (existing.getCount() <= toExtract) {
                this.stacks.set(slot, UnlimitedItemStack.EMPTY);
            } else {
                existing.setCount(existing.getCount() - toExtract);
            }
            this.onContentsChanged(slot, existing);
        }
        return result;
    }

    /**
     * 尝试把物品堆插入到任意匹配或空槽位中，返回未能插入的剩余物品堆。
     */
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int slot = 0; slot < this.size(); slot++) {
            UnlimitedItemStack existing = this.stacks.get(slot);
            if (existing.isEmpty() || existing.isSameItemSameComponents(stack)) {
                stack = this.insertItem(slot, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public UnlimitedItemStack extractUnlimited(int index, int amount, boolean simulate) {
        if (amount <= 0) return UnlimitedItemStack.EMPTY;
        this.validateSlotIndex(index);

        UnlimitedItemStack existing = this.stacks.get(index);
        if (existing.isEmpty()) return UnlimitedItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getCount());
        UnlimitedItemStack result = existing.copyWithCount(toExtract);
        if (!simulate) {
            if (existing.getCount() <= toExtract) {
                this.stacks.set(index, UnlimitedItemStack.EMPTY);
            } else {
                existing.setCount(existing.getCount() - toExtract);
            }
            this.onContentsChanged(index, existing);
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    public int getTypeCount() {
        int count = 0;
        for (UnlimitedItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * This handler has no type limit. Subclasses that expose a finite type limit can override this method.
     */
    public int getTypeLimit() {
        return Integer.MAX_VALUE;
    }

    public double getFullness() {
        double fullness = 0.0;
        for (UnlimitedItemStack stack : this.stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            fullness += (double) stack.getCount() / stack.getMaxStackSize();
        }
        return fullness;
    }

    public void sync(UnlimitedItemStacksResourceHandler items) {
        NonNullList<UnlimitedItemStack> source = UnlimitedItemStacksResourceHandler.trim(items.copyToList());
        NonNullList<UnlimitedItemStack> synced = NonNullList.withSize(this.size(), UnlimitedItemStack.EMPTY);
        for (int index = 0; index < Math.min(source.size(), synced.size()); index++) {
            synced.set(index, source.get(index));
        }
        this.setStacks(synced);
    }

    protected void setStacks(NonNullList<UnlimitedItemStack> stacks) {
        for (int index = 0; index < this.stacks.size(); index++) {
            this.stacks.set(index, index < stacks.size() ? stacks.get(index) : UnlimitedItemStack.EMPTY);
        }
        this.onContentsChanged(-1, UnlimitedItemStack.EMPTY);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag items = new ListTag();
        for (int index = 0; index < this.stacks.size(); index++) {
            UnlimitedItemStack stack = this.stacks.get(index);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", index);
            entry.put("Stack", UnlimitedItemStack.CODEC
                .encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow());
            items.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        NonNullList<UnlimitedItemStack> loaded = NonNullList.create();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt("Slot");
            if (entry.contains("Stack", Tag.TAG_COMPOUND)) {
                UnlimitedItemStack stack = UnlimitedItemStack.CODEC
                    .parse(provider.createSerializationContext(NbtOps.INSTANCE), entry.get("Stack"))
                    .result()
                    .orElse(UnlimitedItemStack.EMPTY);
                while (loaded.size() <= slot) {
                    loaded.add(UnlimitedItemStack.EMPTY);
                }
                loaded.set(slot, stack);
            }
        }
        this.stacks.clear();
        this.stacks.addAll(loaded);
        this.onContentsChanged(-1, UnlimitedItemStack.EMPTY);
    }

    protected NonNullList<UnlimitedItemStack> copyToList() {
        return NonNullList.copyOf(this.stacks);
    }

    protected void onContentsChanged(int index, UnlimitedItemStack original) {
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= this.stacks.size()) {
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
        }
    }

    protected static NonNullList<UnlimitedItemStack> constructStackList(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> result = NonNullList.create();
        result.addAll(from);
        return result;
    }

    protected static NonNullList<UnlimitedItemStack> trim(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> result = NonNullList.create();
        for (UnlimitedItemStack stack : from) {
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }
}
