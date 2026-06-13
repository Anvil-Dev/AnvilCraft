package dev.dubhe.anvilcraft.api.itemhandler;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.network.util.BoolAndInt;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemInstance;
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
import java.util.Optional;
import java.util.function.Function;

public class TypeLimitItemStacksResourceHandler implements ResourceHandler<ItemResource>, ValueIOSerializable {
    public static final String VALUE_IO_KEY = "stacks";
    public static final Codec<NonNullList<UnlimitedItemStack>> STACKS_CODEC = UnlimitedItemStack.CODEC
        .listOf()
        .xmap(TypeLimitItemStacksResourceHandler::constructStackList, Function.identity());
    private final int typeLimit;
    private final int spaceSize;
    private final NonNullList<UnlimitedItemStack> stacks = TypeLimitItemStacksResourceHandler.constructStackList();
    private int space = 0;

    private final ArrayList<StackJournal> snapshotJournals = new ArrayList<>();
    
    public TypeLimitItemStacksResourceHandler(int spaceSize) {
        this(Integer.MAX_VALUE, spaceSize);
    }

    public TypeLimitItemStacksResourceHandler(int typeLimit, int spaceSize) {
        this.typeLimit = typeLimit;
        this.spaceSize = spaceSize;
    }

    private static NonNullList<UnlimitedItemStack> constructStackList() {
        return new NonNullList<>(new ArrayList<>(), UnlimitedItemStack.EMPTY);
    }

    private static NonNullList<UnlimitedItemStack> constructStackList(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> empty = TypeLimitItemStacksResourceHandler.constructStackList();
        empty.addAll(from);
        return empty;
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(this.stacks.get(index).getStack());
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.stacks.get(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    protected int computeEmptySize(ItemResource resource) {
        return TypeLimitItemStacksResourceHandler.computeCount(resource, this.spaceSize - this.space);
    }

    protected int findEmptySlot() {
        for (int i = 0; i < this.stacks.size(); i++) {
            if (this.stacks.get(i).isEmpty()) {
                return i;
            }
        }
        return this.stacks.size() >= this.typeLimit ? -1 : this.stacks.size();
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        int size = this.size();
        for (int index = 0; index < size; index++) {
            BoolAndInt result = this.insertInternal(index, resource, amount, transaction);
            if (result.bool()) {
                return result.integer();
            }
        }

        int index = this.findEmptySlot();
        if (index < 0) {
            return 0;
        }
        if (index == this.stacks.size()) {
            this.stacks.add(UnlimitedItemStack.EMPTY);
            this.updateStacksSize();
        }

        int inserted = Math.min(amount, this.computeEmptySize(resource));
        this.snapshotJournals.get(index).updateSnapshots(transaction);
        this.stacks.set(index, new UnlimitedItemStack(resource, inserted));
        this.space += TypeLimitItemStacksResourceHandler.computeSpace(resource, inserted);
        return inserted;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.insertInternal(index, resource, amount, transaction).integer();
    }

    private BoolAndInt insertInternal(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        UnlimitedItemStack stack = this.stacks.get(index);
        if (!stack.isSameItemSameComponents(resource)) {
            return new BoolAndInt(false, 0);
        }

        int inserted = Math.min(amount, this.computeEmptySize(resource));
        if (inserted <= 0) {
            return new BoolAndInt(true, 0);
        }

        int count = stack.count();
        this.snapshotJournals.get(index).updateSnapshots(transaction);
        this.stacks.set(index, new UnlimitedItemStack(resource, count + inserted));
        this.space += TypeLimitItemStacksResourceHandler.computeSpace(resource, inserted);
        return new BoolAndInt(true, inserted);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        UnlimitedItemStack stack = this.stacks.get(index);
        int count = stack.count();
        if (!stack.isSameItemSameComponents(resource)) {
            return 0;
        }

        int extracted = Math.min(amount, count);
        if (extracted <= 0) {
            return 0;
        }

        this.snapshotJournals.get(index).updateSnapshots(transaction);
        this.stacks.set(index, new UnlimitedItemStack(resource, count - extracted));
        this.space -= TypeLimitItemStacksResourceHandler.computeSpace(resource, extracted);
        return amount;
    }

    private void updateStacksSize() {
        this.snapshotJournals.ensureCapacity(this.stacks.size());
        // Add missing entries
        while (this.snapshotJournals.size() < this.stacks.size()) {
            this.snapshotJournals.add(new StackJournal(this.snapshotJournals.size()));
        }
        // 通常情况下不允许减少快照列表大小。此处将报错
        if (this.snapshotJournals.size() > this.stacks.size()) {
            // this.snapshotJournals.subList(this.stacks.size(), this.snapshotJournals.size()).clear();
            throw new IllegalStateException("Cannot decrease the snapshot journals' size");
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        NonNullList<UnlimitedItemStack> saving = TypeLimitItemStacksResourceHandler.constructStackList();
        for (UnlimitedItemStack stack : this.stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            saving.add(stack);
        }
        output.store(TypeLimitItemStacksResourceHandler.VALUE_IO_KEY, TypeLimitItemStacksResourceHandler.STACKS_CODEC, saving);
    }

    @Override
    public void deserialize(ValueInput input) {
        Optional<NonNullList<UnlimitedItemStack>> stacksOp = input.read(
            TypeLimitItemStacksResourceHandler.VALUE_IO_KEY,
            TypeLimitItemStacksResourceHandler.STACKS_CODEC
        );
        if (stacksOp.isEmpty()) {
            return;
        }
        NonNullList<UnlimitedItemStack> stacks = stacksOp.get();

        // Add missing entries
        while (this.stacks.size() < stacks.size()) {
            this.stacks.add(UnlimitedItemStack.EMPTY);
        }
        if (this.stacks.size() > stacks.size()) {
            this.stacks.subList(stacks.size(), this.stacks.size()).clear();
        }

        this.space = 0;
        for (int i = 0; i < stacks.size(); i++) {
            UnlimitedItemStack stack = stacks.get(i);
            this.stacks.set(i, stack);
            this.space += TypeLimitItemStacksResourceHandler.computeSpace(stack, stack.count());
        }

        this.updateStacksSize();
    }

    @SuppressWarnings("unused")
    protected void onContentsChanged(int index, UnlimitedItemStack original) {
    }

    private class StackJournal extends SnapshotJournal<UnlimitedItemStack> {
        private final int index;

        private StackJournal(int index) {
            this.index = index;
        }

        @Override
        protected UnlimitedItemStack createSnapshot() {
            return TypeLimitItemStacksResourceHandler.this.stacks.get(this.index).copy();
        }

        @Override
        protected void revertToSnapshot(UnlimitedItemStack snapshot) {
            UnlimitedItemStack stack = TypeLimitItemStacksResourceHandler.this.stacks.get(this.index);
            TypeLimitItemStacksResourceHandler.this.stacks.set(this.index, snapshot);
            TypeLimitItemStacksResourceHandler.this.space +=
                TypeLimitItemStacksResourceHandler.computeSpace(snapshot, snapshot.getCount())
                - TypeLimitItemStacksResourceHandler.computeSpace(stack, stack.getCount());
            TypeLimitItemStacksResourceHandler.this.updateStacksSize();
        }

        @Override
        protected void onRootCommit(UnlimitedItemStack originalState) {
            TypeLimitItemStacksResourceHandler.this.onContentsChanged(this.index, originalState);
        }
    }

    public static int computeSpace(ItemResource resource, int count) {
        return Math.ceilDiv(64, resource.getMaxStackSize()) * count;
    }

    public static int computeSpace(ItemInstance instance, int count) {
        return Math.ceilDiv(64, instance.getMaxStackSize()) * count;
    }

    public static int computeCount(ItemResource resource, int space) {
        return Math.floorDiv(space, Math.ceilDiv(64, resource.getMaxStackSize()));
    }

    public static int computeCount(ItemInstance instance, int space) {
        return Math.floorDiv(space, Math.ceilDiv(64, instance.getMaxStackSize()));
    }
}
