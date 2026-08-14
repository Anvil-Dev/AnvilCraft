package dev.dubhe.anvilcraft.api.itemhandler;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.function.Predicate;

public class LargeCauldronInputHandler implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    public static final int SLOT_COUNT = 8;
    public static final int STACK_MULTIPLIER = 9;
    private final Runnable changeListener;
    private NonNullList<UnlimitedItemStack> stacks = NonNullList.withSize(SLOT_COUNT, UnlimitedItemStack.EMPTY);

    public LargeCauldronInputHandler(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        return this.stacks.get(slot).toStack();
    }

    public boolean mutateStackInSlot(int slot, Predicate<ItemStack> mutator) {
        validateSlot(slot);
        UnlimitedItemStack existing = this.stacks.get(slot);
        if (existing.isEmpty()) return false;
        ItemStack stack = existing.getStack().copy();
        if (!mutator.test(stack)) return false;
        existing.setStack(stack);
        this.onContentsChanged();
        return true;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlot(slot);
        if (!stack.isEmpty() && !this.isItemValid(slot, stack)) {
            throw new IllegalArgumentException("Duplicate item in large cauldron input slots");
        }
        int limit = stack.isEmpty() ? 0 : stack.getMaxStackSize() * STACK_MULTIPLIER;
        this.stacks.set(slot, stack.isEmpty()
            ? UnlimitedItemStack.EMPTY
            : new UnlimitedItemStack(stack, Math.min(stack.getCount(), limit)));
        this.onContentsChanged();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.isItemValid(slot, stack)) return stack;
        UnlimitedItemStack existing = this.stacks.get(slot);
        if (!existing.isEmpty() && !existing.isSameItemSameComponents(stack)) return stack;

        int limit = stack.getMaxStackSize() * STACK_MULTIPLIER;
        int accepted = Math.min(stack.getCount(), limit - existing.getCount());
        if (accepted <= 0) return stack;
        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, new UnlimitedItemStack(stack, accepted));
            } else {
                existing.grow(accepted);
            }
            this.onContentsChanged();
        }
        return accepted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlot(slot);
        if (amount <= 0) return ItemStack.EMPTY;
        UnlimitedItemStack existing = this.stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int extracted = Math.min(amount, Math.min(existing.getCount(), existing.getStack().getMaxStackSize()));
        ItemStack result = existing.getStack().copyWithCount(extracted);
        if (!simulate) {
            int remaining = existing.getCount() - extracted;
            this.stacks.set(slot, remaining == 0 ? UnlimitedItemStack.EMPTY : existing.copyWithCount(remaining));
            this.onContentsChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlot(slot);
        UnlimitedItemStack existing = this.stacks.get(slot);
        return (existing.isEmpty() ? 64 : existing.getStack().getMaxStackSize()) * STACK_MULTIPLIER;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlot(slot);
        if (stack.isEmpty()) return false;
        UnlimitedItemStack own = this.stacks.get(slot);
        if (!own.isEmpty() && !own.isSameItemSameComponents(stack)) return false;
        for (int i = 0; i < this.stacks.size(); i++) {
            if (i == slot) continue;
            UnlimitedItemStack other = this.stacks.get(i);
            if (!other.isEmpty() && other.isSameItemSameComponents(stack)) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return this.stacks.stream().allMatch(UnlimitedItemStack::isEmpty);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag items = new ListTag();
        for (int slot = 0; slot < this.stacks.size(); slot++) {
            UnlimitedItemStack stack = this.stacks.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            items.add(UnlimitedItemStack.CODEC.encode(stack, provider.createSerializationContext(NbtOps.INSTANCE), entry).getOrThrow());
        }
        CompoundTag result = new CompoundTag();
        result.put("Items", items);
        return result;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.stacks = NonNullList.withSize(SLOT_COUNT, UnlimitedItemStack.EMPTY);
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= SLOT_COUNT) continue;
            UnlimitedItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), entry)
                .result()
                .ifPresent(stack -> this.stacks.set(slot, stack));
        }
        this.onContentsChanged();
    }

    private void onContentsChanged() {
        this.changeListener.run();
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) throw new IndexOutOfBoundsException("Input slot " + slot);
    }
}
