package dev.dubhe.anvilcraft.api.itemhandler;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class LargeCauldronInputHandler implements ResourceHandler<ItemResource>, ValueIOSerializable {
    public static final int SLOT_COUNT = 8;
    public static final int STACK_MULTIPLIER = 9;
    private static final Codec<List<UnlimitedItemStack>> STACKS_CODEC = UnlimitedItemStack.OPTIONAL_CODEC.listOf();

    private final Runnable changeListener;
    private final NonNullList<UnlimitedItemStack> stacks = NonNullList.withSize(
        LargeCauldronInputHandler.SLOT_COUNT,
        UnlimitedItemStack.EMPTY
    );
    private final List<StackJournal> snapshotJournals = new ArrayList<>(LargeCauldronInputHandler.SLOT_COUNT);

    public LargeCauldronInputHandler(Runnable changeListener) {
        this.changeListener = changeListener;
        for (int slot = 0; slot < LargeCauldronInputHandler.SLOT_COUNT; slot++) {
            this.snapshotJournals.add(new StackJournal(slot));
        }
    }

    @Override
    public int size() {
        return LargeCauldronInputHandler.SLOT_COUNT;
    }

    @Override
    public ItemResource getResource(int index) {
        Objects.checkIndex(index, LargeCauldronInputHandler.SLOT_COUNT);
        return ItemResource.of(this.stacks.get(index).getStack());
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, LargeCauldronInputHandler.SLOT_COUNT);
        return this.stacks.get(index).count();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        Objects.checkIndex(index, LargeCauldronInputHandler.SLOT_COUNT);
        return resource.isEmpty() ? 0 : (long) resource.getMaxStackSize() * LargeCauldronInputHandler.STACK_MULTIPLIER;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        Objects.checkIndex(index, LargeCauldronInputHandler.SLOT_COUNT);
        if (resource.isEmpty()) return false;
        ItemResource own = this.getResource(index);
        if (!own.isEmpty() && !own.equals(resource)) return false;
        for (int slot = 0; slot < LargeCauldronInputHandler.SLOT_COUNT; slot++) {
            if (slot != index && this.getResource(slot).equals(resource)) return false;
        }
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, LargeCauldronInputHandler.SLOT_COUNT);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!this.isValid(index, resource)) return 0;

        int stored = this.getAmountAsInt(index);
        int inserted = Math.min(amount, this.getCapacityAsInt(index, resource) - stored);
        if (inserted <= 0) return 0;
        this.snapshotJournals.get(index).updateSnapshots(transaction);
        this.stacks.set(index, new UnlimitedItemStack(resource, stored + inserted));
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, LargeCauldronInputHandler.SLOT_COUNT);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!this.getResource(index).equals(resource)) return 0;

        int stored = this.getAmountAsInt(index);
        int extracted = Math.min(amount, stored);
        if (extracted <= 0) return 0;
        this.snapshotJournals.get(index).updateSnapshots(transaction);
        int remaining = stored - extracted;
        this.stacks.set(index, remaining == 0 ? UnlimitedItemStack.EMPTY : new UnlimitedItemStack(resource, remaining));
        return extracted;
    }

    public ItemStack getStackInSlot(int slot) {
        ItemResource resource = this.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(this.getAmountAsInt(slot));
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        Objects.checkIndex(slot, LargeCauldronInputHandler.SLOT_COUNT);
        ItemResource resource = ItemResource.of(stack);
        if (!resource.isEmpty() && !this.isValid(slot, resource)) {
            throw new IllegalArgumentException("Duplicate item in large cauldron input slots");
        }
        int amount = Math.min(stack.getCount(), this.getCapacityAsInt(slot, resource));
        this.stacks.set(slot, amount == 0 ? UnlimitedItemStack.EMPTY : new UnlimitedItemStack(resource, amount));
        this.changeListener.run();
    }

    public boolean mutateStackInSlot(int slot, Predicate<ItemStack> mutator) {
        ItemResource resource = this.getResource(slot);
        if (resource.isEmpty()) return false;
        ItemStack stack = resource.toStack(1);
        if (!mutator.test(stack)) return false;
        ItemResource updated = ItemResource.of(stack);
        if (updated.isEmpty() || !this.isValid(slot, updated)) return false;
        this.stacks.set(slot, new UnlimitedItemStack(updated, this.getAmountAsInt(slot)));
        this.changeListener.run();
        return true;
    }

    public boolean isEmpty() {
        return this.stacks.stream().allMatch(UnlimitedItemStack::isEmpty);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("Items", LargeCauldronInputHandler.STACKS_CODEC, this.stacks);
    }

    @Override
    public void deserialize(ValueInput input) {
        List<UnlimitedItemStack> loaded = input.read("Items", LargeCauldronInputHandler.STACKS_CODEC).orElse(List.of());
        for (int slot = 0; slot < LargeCauldronInputHandler.SLOT_COUNT; slot++) {
            UnlimitedItemStack stack = slot < loaded.size() ? loaded.get(slot) : UnlimitedItemStack.EMPTY;
            this.stacks.set(slot, stack.copy());
        }
        this.changeListener.run();
    }

    private class StackJournal extends SnapshotJournal<UnlimitedItemStack> {
        private final int slot;

        private StackJournal(int slot) {
            this.slot = slot;
        }

        @Override
        protected UnlimitedItemStack createSnapshot() {
            return LargeCauldronInputHandler.this.stacks.get(this.slot).copy();
        }

        @Override
        protected void revertToSnapshot(UnlimitedItemStack snapshot) {
            LargeCauldronInputHandler.this.stacks.set(this.slot, snapshot);
        }

        @Override
        protected void onRootCommit(UnlimitedItemStack originalState) {
            LargeCauldronInputHandler.this.changeListener.run();
        }
    }
}
