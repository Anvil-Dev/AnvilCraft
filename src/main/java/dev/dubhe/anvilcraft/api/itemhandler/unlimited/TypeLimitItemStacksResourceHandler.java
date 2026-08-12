package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntUnaryOperator;

public class TypeLimitItemStacksResourceHandler extends UnlimitedItemStacksResourceHandler {
    public static final String TYPE_LIMIT_KEY = "type_limit";
    public static final String SPACE_SIZE_KEY = SpaceSizeItemStacksResourceHandler.SPACE_SIZE_KEY;

    @Getter
    @Setter
    private int typeLimit;
    @Getter
    private int spaceSize;

    public TypeLimitItemStacksResourceHandler(int spaceSize) {
        this(Integer.MAX_VALUE, spaceSize);
    }

    public TypeLimitItemStacksResourceHandler(int typeLimit, int spaceSize) {
        super(0);
        this.typeLimit = checkTypeLimit(typeLimit);
        this.spaceSize = checkSpaceSize(spaceSize);
    }

    public TypeLimitItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks, int typeLimit, int spaceSize) {
        super(TypeLimitItemStacksResourceHandler.trim(typeLimit, spaceSize, stacks));
        this.typeLimit = checkTypeLimit(typeLimit);
        this.spaceSize = checkSpaceSize(spaceSize);
    }

    private static int checkTypeLimit(int typeLimit) {
        if (typeLimit < 0) {
            throw new IllegalArgumentException("Type limit cannot be negative: " + typeLimit);
        }
        return typeLimit;
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
            // 增长槽：仅在未超类型上限且空间允许时扩展，否则原样返回，
            // 避免 ItemHandlerHelper 遍历循环因 getSlots() 增长而无限循环
            if (slot >= this.getSlots() || this.size() >= this.typeLimit) {
                return stack;
            }
            long capacity = TypeLimitItemStacksResourceHandler.computeCount(stack, this.spaceSize);
            int amount = (int) Math.min(stack.getCount(), Math.max(0, capacity));
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
        UnlimitedItemStack existing = this.stacks.get(slot);
        int matchingIndex = this.findMatchingSlot(stack);
        if (existing.isEmpty() && matchingIndex >= 0 && matchingIndex != slot) {
            return stack;
        }
        if (!existing.isEmpty() && !existing.isSameItemSameComponents(stack)) {
            return stack;
        }
        long capacity = TypeLimitItemStacksResourceHandler.computeCount(stack, this.spaceSize);
        if (capacity < 1) {
            return stack;
        }
        long currentForType = 0;
        for (UnlimitedItemStack item : this.stacks) {
            if (!item.isEmpty() && item.isSameItemSameComponents(stack)) {
                currentForType += item.getCount();
            }
        }
        int amount = (int) Math.min(stack.getCount(), Math.max(0, capacity - currentForType));
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
        int matchingIndex = this.findMatchingSlot(stack);
        if (matchingIndex >= 0) {
            return this.insertItem(matchingIndex, stack, simulate);
        }
        int newIndex = this.findNewSlot();
        if (newIndex < 0) {
            return stack;
        }
        if (newIndex == this.size()) {
            if (simulate) {
                return stack;
            }
            this.stacks.add(UnlimitedItemStack.EMPTY);
        }
        return this.insertItem(newIndex, stack, simulate);
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
        tag.putInt(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY, this.typeLimit);
        tag.putInt(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY, this.spaceSize);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY, Tag.TAG_INT)) {
            this.typeLimit = checkTypeLimit(tag.getInt(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY));
        }
        if (tag.contains(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY, Tag.TAG_INT)) {
            this.spaceSize = checkSpaceSize(tag.getInt(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY));
        }
        super.deserializeNBT(provider, tag);
    }

    public int getSpace() {
        int space = 0;
        for (UnlimitedItemStack stack : this.stacks) {
            space = (int) Math.min(
                Integer.MAX_VALUE,
                (long) space + TypeLimitItemStacksResourceHandler.computeSpace(stack, stack.getCount())
            );
        }
        return space;
    }

    @Override
    public double getFullness() {
        if (this.typeLimit == 0 || this.spaceSize == 0) {
            return 0;
        }
        return this.getSpace() / ((double) this.typeLimit * this.spaceSize);
    }

    @Override
    public void sync(UnlimitedItemStacksResourceHandler items) {
        if (items instanceof TypeLimitItemStacksResourceHandler typeHandler) {
            this.spaceSize = Math.max(this.spaceSize, typeHandler.spaceSize);
        }
        this.setStacks(TypeLimitItemStacksResourceHandler.trim(this.typeLimit, this.spaceSize, items.copyToList()));
    }

    protected int computeEmptySize(ItemStack stack) {
        return TypeLimitItemStacksResourceHandler.computeCount(stack, this.spaceSize);
    }

    protected int findNewSlot() {
        for (int index = 0; index < this.size(); index++) {
            if (this.stacks.get(index).isEmpty()) {
                return index;
            }
        }
        return this.size() < this.typeLimit ? this.size() : -1;
    }

    private int findMatchingSlot(ItemStack stack) {
        for (int index = 0; index < this.size(); index++) {
            if (this.stacks.get(index).isSameItemSameComponents(stack)) {
                return index;
            }
        }
        return -1;
    }

    private static int findMatchingSlot(List<UnlimitedItemStack> stacks, UnlimitedItemStack target) {
        for (int index = 0; index < stacks.size(); index++) {
            if (stacks.get(index).isSameItemSameComponents(target)) {
                return index;
            }
        }
        return -1;
    }

    private static NonNullList<UnlimitedItemStack> trim(
        int typeLimit,
        int spaceSize,
        List<UnlimitedItemStack> stacks
    ) {
        checkTypeLimit(typeLimit);
        checkSpaceSize(spaceSize);
        NonNullList<UnlimitedItemStack> result = NonNullList.create();
        for (UnlimitedItemStack input : stacks) {
            if (input.isEmpty()) {
                continue;
            }

            int existingIndex = TypeLimitItemStacksResourceHandler.findMatchingSlot(result, input);
            if (existingIndex < 0 && result.size() >= typeLimit) {
                continue;
            }

            int capacity = TypeLimitItemStacksResourceHandler.computeCount(input, spaceSize);
            if (existingIndex >= 0) {
                UnlimitedItemStack existing = result.get(existingIndex);
                long mergedCount = (long) existing.getCount() + input.getCount();
                existing.setCount((int) Math.min(capacity, mergedCount));
            } else {
                UnlimitedItemStack accepted = input.copy();
                accepted.setCount(Math.min(capacity, input.getCount()));
                if (!accepted.isEmpty()) {
                    result.add(accepted);
                }
            }
        }
        return result;
    }

    public static int computeSpace(ItemStack stack, int count) {
        return SpaceSizeItemStacksResourceHandler.computeSpace(stack, count);
    }

    public static int computeSpace(UnlimitedItemStack stack, int count) {
        return SpaceSizeItemStacksResourceHandler.computeSpace(stack, count);
    }

    public static int computeCount(ItemStack stack, int space) {
        return SpaceSizeItemStacksResourceHandler.computeCount(stack, space);
    }

    public static int computeCount(UnlimitedItemStack stack, int space) {
        return SpaceSizeItemStacksResourceHandler.computeCount(stack, space);
    }
}
