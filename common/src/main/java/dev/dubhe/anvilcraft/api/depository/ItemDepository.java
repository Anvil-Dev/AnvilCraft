package dev.dubhe.anvilcraft.api.depository;

import dev.dubhe.anvilcraft.api.INamedTagSerializable;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public class ItemDepository implements IItemDepository, INamedTagSerializable {
    private final NonNullList<ItemStack> stacks;

    public ItemDepository(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int getSlots() {
        return this.stacks.size();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.stacks.get(slot);
    }

    @Override
    public ItemStack insert(int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges) {
        this.validateSlotIndex(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.isItemValid(slot, stack)) return stack;
        ItemStack stackInSlot = this.getStack(slot);
        int limit = this.getSlotLimit(slot);
        if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameTags(stackInSlot, stack)) return stack;
        limit -= stackInSlot.getCount();
        if (limit <= 0) return stack;
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (stackInSlot.isEmpty()) {
                this.stacks.set(slot, reachedLimit ? ItemDepositoryHelper.copyStackWithSize(stack, limit) : stack);
            } else {
                stackInSlot.grow(reachedLimit ? limit : stack.getCount());
            }
            if (notifyChanges) {
                this.onContentsChanged(slot);
            }
        }
        return reachedLimit ? ItemDepositoryHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate, boolean notifyChanges) {
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @SuppressWarnings("unused")
    public void onContentsChanged(int slot) {
        IItemDepository.super.onContentsChanged();
    }

    @Override
    public @NotNull CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        ListTag listTag = new ListTag();
        int slots = this.getSlots();
        compoundTag.putInt("Size", slots);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = this.getStack(i);
            if (stack.isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            stack.save(itemTag);
            listTag.add(itemTag);
        }
        if (!listTag.isEmpty()) compoundTag.put("Items", listTag);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(@NotNull CompoundTag tag) {
        if (!tag.contains("Items")) return;
        ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
        int slots = this.getSlots();
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= slots) continue;
            this.stacks.set(slot, ItemStack.of(itemTag));
        }
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size())
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public ItemStack clearItem(int slot) {
        ItemStack stack = this.getStack(slot);
        this.stacks.set(slot, ItemStack.EMPTY);
        return stack;
    }

    public void setItem(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
    }

    public static class Pollable extends ItemDepository {
        public Pollable(int size) {
            super(size);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getEmptyOrSmallerSlot(stack) == slot;
        }

        private int getEmptyOrSmallerSlot(ItemStack stack) {
            int slotCount = this.getSlots();
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = 0; i < slotCount; i++) {
                ItemStack stackInSlot = this.getStack(i);
                if (stackInSlot.isEmpty()) return i;
                if (!ItemStack.isSameItemSameTags(stackInSlot, stack)) continue;
                int stackInSlotCount = stackInSlot.getCount();
                if (stackInSlotCount < countInSlot && stackInSlotCount < this.getSlotLimit(i)) {
                    slot = i;
                    countInSlot = stackInSlotCount;
                }
            }
            return slot;
        }
    }
}
