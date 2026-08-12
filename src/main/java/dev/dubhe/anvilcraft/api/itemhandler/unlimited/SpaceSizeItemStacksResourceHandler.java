package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntUnaryOperator;

public class SpaceSizeItemStacksResourceHandler extends UnlimitedItemStacksResourceHandler {
    public static final String SPACE_SIZE_KEY = "space_size";

    @Getter
    private int spaceSize;

    public SpaceSizeItemStacksResourceHandler(int spaceSize) {
        this(spaceSize, UnlimitedItemStacksResourceHandler.constructStackList(List.of()));
    }

    public SpaceSizeItemStacksResourceHandler(int spaceSize, NonNullList<UnlimitedItemStack> stacks) {
        super(SpaceSizeItemStacksResourceHandler.trim(spaceSize, stacks));
        this.spaceSize = checkSpaceSize(spaceSize);
    }

    public SpaceSizeItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks, int spaceSize) {
        this(spaceSize, stacks);
    }

    private static int checkSpaceSize(int spaceSize) {
        if (spaceSize < 0) {
            throw new IllegalArgumentException("Space size cannot be negative: " + spaceSize);
        }
        return spaceSize;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot >= this.stacks.size()) {
            // 增长槽：仅在空间允许时扩展，否则原样返回，避免 ItemHandlerHelper 遍历循环无限增长
            if (slot >= this.getSlots()) {
                return stack;
            }
            int remainingSpace = this.spaceSize - this.getSpace();
            long fit = SpaceSizeItemStacksResourceHandler.computeCount(stack, remainingSpace);
            int amount = (int) Math.min(stack.getCount(), fit);
            if (amount <= 0) {
                return stack;
            }
            if (simulate) {
                ItemStack leftover = stack.copy();
                leftover.shrink(amount);
                return leftover;
            }
            this.ensureSlot(slot);
        }
        this.validateSlotIndex(slot);
        UnlimitedItemStack existing = this.stacks.get(slot);
        if (!existing.isEmpty() && !existing.isSameItemSameComponents(stack)) {
            return stack;
        }
        int remainingSpace = this.spaceSize - this.getSpace();
        long fit = SpaceSizeItemStacksResourceHandler.computeCount(stack, remainingSpace);
        if (fit < 1) {
            return stack;
        }
        int amount = (int) Math.min(stack.getCount(), fit);
        if (amount <= 0) {
            return stack;
        }
        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, new UnlimitedItemStack(stack.copyWithCount(amount)));
            } else {
                existing.setCount(existing.getCount() + amount);
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
        for (int slot = 0; slot < this.size(); slot++) {
            UnlimitedItemStack existing = this.stacks.get(slot);
            if (existing.isEmpty() || existing.isSameItemSameComponents(stack)) {
                stack = this.insertItem(slot, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int remainingSpace = this.spaceSize - this.getSpace();
        if (SpaceSizeItemStacksResourceHandler.computeCount(stack, remainingSpace) < 1) {
            return stack;
        }
        if (simulate) {
            return stack;
        }
        this.stacks.add(UnlimitedItemStack.EMPTY);
        return this.insertItem(this.size() - 1, stack, false);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return super.extractItem(slot, amount, simulate);
    }

    public void addSpaceSize(IntUnaryOperator adder) {
        int newSpaceSize = adder.applyAsInt(this.spaceSize);
        if (newSpaceSize >= this.spaceSize) {
            this.spaceSize = checkSpaceSize(newSpaceSize);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = super.serializeNBT(provider);
        tag.putInt(SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY, this.spaceSize);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains(SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY, Tag.TAG_INT)) {
            this.spaceSize = checkSpaceSize(tag.getInt(SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY));
        }
        super.deserializeNBT(provider, tag);
    }

    public int getSpace() {
        int space = 0;
        for (UnlimitedItemStack stack : this.stacks) {
            space = (int) Math.min(
                Integer.MAX_VALUE,
                (long) space + SpaceSizeItemStacksResourceHandler.computeSpace(stack, stack.getCount())
            );
        }
        return space;
    }

    @Override
    public double getFullness() {
        return this.spaceSize == 0 ? 0 : (double) this.getSpace() / this.spaceSize;
    }

    @Override
    public void sync(UnlimitedItemStacksResourceHandler items) {
        if (!(items instanceof SpaceSizeItemStacksResourceHandler spaceHandler)) {
            super.sync(items);
            return;
        }
        this.spaceSize = spaceHandler.spaceSize;
        this.setStacks(SpaceSizeItemStacksResourceHandler.trim(this.spaceSize, items.copyToList()));
    }

    private static NonNullList<UnlimitedItemStack> trim(int spaceSize, List<UnlimitedItemStack> stacks) {
        checkSpaceSize(spaceSize);
        NonNullList<UnlimitedItemStack> result = NonNullList.create();
        int usedSpace = 0;
        for (UnlimitedItemStack input : stacks) {
            if (input.isEmpty()) {
                continue;
            }

            int remainingSpace = spaceSize - usedSpace;
            int accepted = Math.min(
                input.getCount(),
                SpaceSizeItemStacksResourceHandler.computeCount(input, remainingSpace)
            );
            if (accepted <= 0) {
                continue;
            }

            UnlimitedItemStack acceptedStack = input.copy();
            acceptedStack.setCount(accepted);
            usedSpace += SpaceSizeItemStacksResourceHandler.computeSpace(acceptedStack, accepted);
            boolean merged = false;
            for (UnlimitedItemStack existing : result) {
                if (existing.isSameItemSameComponents(acceptedStack)) {
                    existing.setCount(existing.getCount() + accepted);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                result.add(acceptedStack);
            }
        }
        return result;
    }

    public static int computeSpace(ItemStack stack, int count) {
        return computeSpace(stack.getMaxStackSize(), count);
    }

    public static int computeSpace(UnlimitedItemStack stack, int count) {
        return computeSpace(stack.getMaxStackSize(), count);
    }

    private static int computeSpace(int maxStackSize, int count) {
        long space = (long) Math.ceilDiv(64, maxStackSize) * count;
        return (int) Math.min(Integer.MAX_VALUE, space);
    }

    public static int computeCount(ItemStack stack, int space) {
        return computeCount(stack.getMaxStackSize(), space);
    }

    public static int computeCount(UnlimitedItemStack stack, int space) {
        return computeCount(stack.getMaxStackSize(), space);
    }

    private static int computeCount(int maxStackSize, int space) {
        return Math.floorDiv(space, Math.ceilDiv(64, maxStackSize));
    }
}
