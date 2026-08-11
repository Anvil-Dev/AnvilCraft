package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class UnlimitedItemStacksResourceHandler implements IItemHandler {
    public static final String STACKS_KEY = "stacks";
    public static final Codec<NonNullList<UnlimitedItemStack>> STACKS_CODEC = UnlimitedItemStack.CODEC
        .listOf()
        .xmap(UnlimitedItemStacksResourceHandler::constructStackList, UnlimitedItemStacksResourceHandler::trim);
    public static final MapCodec<UnlimitedItemStacksResourceHandler> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UnlimitedItemStacksResourceHandler.STACKS_CODEC
            .fieldOf(UnlimitedItemStacksResourceHandler.STACKS_KEY)
            .forGetter(UnlimitedItemStacksResourceHandler::copyToList)
    ).apply(ins, UnlimitedItemStacksResourceHandler::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<UnlimitedItemStack>> STACKS_STREAM_CODEC =
        UnlimitedItemStack.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(UnlimitedItemStacksResourceHandler::constructStackList, UnlimitedItemStacksResourceHandler::trim);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStacksResourceHandler> STREAM_CODEC =
        UnlimitedItemStacksResourceHandler.STACKS_STREAM_CODEC
            .map(UnlimitedItemStacksResourceHandler::new, UnlimitedItemStacksResourceHandler::copyToList);

    protected final NonNullList<UnlimitedItemStack> stacks;

    public UnlimitedItemStacksResourceHandler(int size) {
        this.stacks = NonNullList.withSize(size, UnlimitedItemStack.EMPTY);
    }

    public UnlimitedItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks) {
        this.stacks = NonNullList.copyOf(stacks);
    }

    public int size() {
        return this.stacks.size();
    }

    @Override
    public int getSlots() {
        return this.size();
    }

    public UnlimitedItemStack getUnlimitedStackInSlot(int index) {
        return this.stacks.get(index);
    }

    public long getAmountAsLong(int index) {
        return this.stacks.get(index).getCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.stacks.get(slot).toStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.isItemValid(slot, stack)) return stack;
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
        this.validateSlotIndex(slot);

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
        NonNullList<UnlimitedItemStack> result = new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
        result.addAll(from);
        return result;
    }

    protected static NonNullList<UnlimitedItemStack> trim(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> result = new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
        for (UnlimitedItemStack stack : from) {
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }
}