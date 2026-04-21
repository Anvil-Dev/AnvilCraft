package dev.dubhe.anvilcraft.api.itemhandler;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class OverLimitItemHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    private final int baseLimit;
    private NonNullList<UnlimitedItemStack> stacks;

    public OverLimitItemHandler(int baseLimit) {
        this(baseLimit, 1);
    }

    public OverLimitItemHandler(int baseLimit, int size) {
        this.stacks = NonNullList.withSize(size, UnlimitedItemStack.EMPTY);
        this.baseLimit = baseLimit;
    }

    public OverLimitItemHandler(int baseLimit, NonNullList<ItemStack> stacks) {
        this.stacks = NonNullList.createWithCapacity(stacks.size());
        for (int i = 0, stacksSize = stacks.size(); i < stacksSize; i++) {
            this.stacks.add(i, new UnlimitedItemStack(stacks.get(i)));
        }
        this.baseLimit = baseLimit;
    }

    public OverLimitItemHandler(NonNullList<UnlimitedItemStack> stacks, int baseLimit) {
        this.stacks = NonNullList.copyOf(stacks);
        this.baseLimit = baseLimit;
    }

    @Override
    public int getSlots() {
        return this.stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.getUnlimitedStackInSlot(slot).toStack();
    }

    public UnlimitedItemStack getUnlimitedStackInSlot(int slot) {
        return this.stacks.get(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        this.stacks.set(slot, new UnlimitedItemStack(stack));
    }

    public void setUnlimitedStackInSlot(int slot, UnlimitedItemStack stack) {
        this.stacks.set(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.isItemValid(slot, stack)) return stack;
        this.validateSlotIndex(slot);

        UnlimitedItemStack existing = this.stacks.get(slot);

        int limit = this.getSlotLimit(slot);
        if (!existing.isEmpty()) {
            if (!existing.isSameItemSameComponents(stack)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;

        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            int maxLimit = this.getSlotLimit(slot);
            if (existing.isEmpty()) {
                this.stacks.set(slot, reachedLimit ? new UnlimitedItemStack(stack, maxLimit) : new UnlimitedItemStack(stack));
            } else {
                existing.grow(reachedLimit ? maxLimit : stack.getCount());
            }
            this.onContentsChanged(slot);
        }

        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        this.validateSlotIndex(slot);

        UnlimitedItemStack existing = this.stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getStack().getMaxStackSize());

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.stacks.set(slot, UnlimitedItemStack.EMPTY);
                this.onContentsChanged(slot);
            }
            return existing.toStack();
        } else {
            if (!simulate) {
                this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
                this.onContentsChanged(slot);
            }
            return existing.copyWithCount(toExtract).toStack();
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.stacks.get(slot).getStack().getMaxStackSize() * this.baseLimit;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    public boolean isEmpty() {
        for (UnlimitedItemStack stack : this.stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag itemsTag = new ListTag();
        for (int i = 0; i < this.stacks.size(); i++) {
            UnlimitedItemStack stack = this.stacks.get(i);
            if (stack.isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            itemTag.merge(stack.serializeNBT(provider));
            itemsTag.add(itemTag);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", itemsTag);
        nbt.putInt("Size", this.stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : this.stacks.size());
        ListTag itemsTag = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            CompoundTag itemTag = itemsTag.getCompound(i);
            int slot = itemTag.getInt("Slot");

            if (slot < 0 || slot >= this.stacks.size()) continue;
            UnlimitedItemStack.parse(provider, itemTag).ifPresent(stack -> this.stacks.set(slot, stack));
        }
        this.onLoad();
    }

    public void setSize(int size) {
        this.stacks = NonNullList.withSize(size, UnlimitedItemStack.EMPTY);
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= this.stacks.size()) {
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
        }
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OverLimitItemHandler that)) return false;
        return this.baseLimit == that.baseLimit && UnlimitedItemStack.listMatches(this.stacks, that.stacks);
    }

    @Override
    public int hashCode() {
        return this.baseLimit * 31 + UnlimitedItemStack.hashStackList(this.stacks);
    }
}