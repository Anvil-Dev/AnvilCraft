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
    public void setStack(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
    }

    @Override
    public ItemStack insert(
        int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer
    ) {
        this.validateSlotIndex(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (isServer && !this.isItemValid(slot, stack)) return stack;
        if (!this.canPlaceItem(slot, stack)) return stack;
        ItemStack stackInSlot = this.getStack(slot);
        int limit = this.getSlotLimit(slot);
        if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameTags(stackInSlot, stack)) return stack;
        limit = Math.min(limit, stackInSlot.getItem().getMaxStackSize());
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
        if (amount == 0) return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = this.getStack(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getCount());

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.stacks.set(slot, ItemStack.EMPTY);
                if (notifyChanges) {
                    onContentsChanged(slot);
                }
            } else {
                return existing.copy();
            }
        } else {
            if (!simulate) {
                this.stacks.set(slot,
                    ItemDepositoryHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                if (notifyChanges) {
                    onContentsChanged(slot);
                }
            }
        }
        return ItemDepositoryHelper.copyStackWithSize(existing, toExtract);
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
    public @NotNull CompoundTag serializeNbt() {
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
    public void deserializeNbt(@NotNull CompoundTag tag) {
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

    /**
     * @return 是否为空
     */
    public boolean isEmpty() {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    /**
     * 清除物品
     *
     * @param slot 槽位
     * @return 被清除的物品
     */
    public ItemStack clearItem(int slot) {
        ItemStack stack = this.getStack(slot);
        this.stacks.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @SuppressWarnings("unused")
    public static class Pollable extends ItemDepository {
        public Pollable(int size) {
            super(size);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getEmptyOrSmallerSlot(stack) == slot;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return super.isItemValid(slot, stack);
        }

        protected int getEmptyOrSmallerSlot(ItemStack stack) {
            int slotCount = this.getSlots();
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = slotCount - 1; i >= 0; i--) {
                ItemStack stackInSlot = this.getStack(i);
                if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameTags(stackInSlot, stack)) continue;
                int stackInSlotCount = stackInSlot.getCount();
                if (stackInSlotCount <= countInSlot && stackInSlotCount < this.getSlotLimit(i)) {
                    slot = i;
                    countInSlot = stackInSlotCount;
                }
            }
            return slot;
        }
    }
}
