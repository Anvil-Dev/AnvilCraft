package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UnlimitedItemStacksResourceHandler extends StacksResourceHandler<UnlimitedItemStack, ItemResource> {
    public static final String STACKS_KEY = "stacks";
    public static final Codec<NonNullList<UnlimitedItemStack>> STACKS_CODEC = UnlimitedItemStack.OPTIONAL_CODEC
        .listOf()
        .xmap(UnlimitedItemStacksResourceHandler::constructStackList, UnlimitedItemStacksResourceHandler::trim);
    public static final MapCodec<UnlimitedItemStacksResourceHandler> CODEC = CodecUtil.mapCodec(
        UnlimitedItemStacksResourceHandler.STACKS_CODEC
            .fieldOf(UnlimitedItemStacksResourceHandler.STACKS_KEY)
            .forGetter(UnlimitedItemStacksResourceHandler::copyToList),
        UnlimitedItemStacksResourceHandler::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<UnlimitedItemStack>> STACKS_STREAM_CODEC =
        UnlimitedItemStack.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(UnlimitedItemStacksResourceHandler::constructStackList, UnlimitedItemStacksResourceHandler::trim);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStacksResourceHandler> STREAM_CODEC =
        UnlimitedItemStacksResourceHandler.STACKS_STREAM_CODEC
            .map(UnlimitedItemStacksResourceHandler::new, UnlimitedItemStacksResourceHandler::copyToList);

    private final ItemStorageIndex<ItemResource> itemIndex = new ItemStorageIndex<>();
    private final Map<Integer, SnapshotJournal<UnlimitedItemStack>> indexJournals = new HashMap<>();

    public UnlimitedItemStacksResourceHandler(int size) {
        super(size, UnlimitedItemStack.EMPTY, UnlimitedItemStack.OPTIONAL_CODEC);
        this.rebuildIndex();
    }

    public UnlimitedItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks) {
        super(constructStackList(stacks.stream().map(UnlimitedItemStack::copy).toList()),
            UnlimitedItemStack.EMPTY, UnlimitedItemStack.OPTIONAL_CODEC);
        this.rebuildIndex();
    }

    @Override
    protected ItemResource getResourceFrom(UnlimitedItemStack stack) {
        return ItemResource.of(stack.getStack());
    }

    @Override
    protected int getAmountFrom(UnlimitedItemStack stack) {
        return stack.getCount();
    }

    @Override
    protected UnlimitedItemStack getStackFrom(ItemResource resource, int amount) {
        return amount == 0 ? UnlimitedItemStack.EMPTY : new UnlimitedItemStack(resource, amount);
    }

    @Override
    protected UnlimitedItemStack copyOf(UnlimitedItemStack stack) {
        return stack.copy();
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean matches(UnlimitedItemStack stack, ItemResource resource) {
        return stack.isSameItemSameComponents(resource);
    }

    public int getTypeCount() {
        return this.itemIndex.entries();
    }

    /** 指定物品的数量由单槽变更维护，查询不扫描库存。 */
    public long count(ItemResource resource) {
        return this.itemIndex.count(resource);
    }

    public int findSlot(ItemResource resource) {
        return this.itemIndex.first(resource);
    }

    protected int firstEmptySlot() {
        return this.itemIndex.emptySlot();
    }

    protected int indexedSpace() {
        return this.itemIndex.space();
    }

    private void updateIndex(int slot, UnlimitedItemStack stack) {
        this.itemIndex.update(slot, stack.isEmpty() ? null : ItemResource.of(stack.getStack()),
            stack.getCount(), stack.isEmpty() ? 64 : stack.getStack().getMaxStackSize());
    }

    private void rebuildIndex() {
        this.itemIndex.clear();
        for (int slot = 0; slot < this.stacks.size(); slot++) {
            this.updateIndex(slot, this.stacks.get(slot));
        }
    }

    private void snapshotIndex(int slot, TransactionContext transaction) {
        this.indexJournals.computeIfAbsent(slot, index -> new SnapshotJournal<UnlimitedItemStack>() {
            @Override
            protected UnlimitedItemStack createSnapshot() {
                return UnlimitedItemStacksResourceHandler.this.stacks.get(index).copy();
            }

            @Override
            protected void revertToSnapshot(UnlimitedItemStack snapshot) {
                UnlimitedItemStacksResourceHandler.this.updateIndex(index, snapshot);
            }
        }).updateSnapshots(transaction);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        this.snapshotIndex(index, transaction);
        int inserted = super.insert(index, resource, amount, transaction);
        this.updateIndex(index, this.stacks.get(index));
        return inserted;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int inserted = 0;
        for (int slot : this.itemIndex.slots(resource)) {
            inserted += this.insert(slot, resource, amount - inserted, transaction);
            if (inserted == amount) {
                return inserted;
            }
        }
        while (inserted < amount && this.firstEmptySlot() < this.size()) {
            int accepted = this.insert(this.firstEmptySlot(), resource, amount - inserted, transaction);
            if (accepted == 0) {
                break;
            }
            inserted += accepted;
        }
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        this.snapshotIndex(index, transaction);
        int extracted = super.extract(index, resource, amount, transaction);
        this.updateIndex(index, this.stacks.get(index));
        return extracted;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int extracted = 0;
        for (int slot : this.itemIndex.slots(resource)) {
            extracted += this.extract(slot, resource, amount - extracted, transaction);
            if (extracted == amount) {
                break;
            }
        }
        return extracted;
    }

    @Override
    public void set(int slot, ItemResource resource, int amount) {
        Objects.checkIndex(slot, this.size());
        TransferPreconditions.checkNonNegative(amount);
        if (resource.isEmpty() && amount > 0) {
            throw new IllegalArgumentException("Resource is empty but the amount is positive: " + amount);
        }
        UnlimitedItemStack old = this.stacks.set(slot, this.getStackFrom(resource, amount));
        this.updateIndex(slot, this.stacks.get(slot));
        this.onContentsChanged(slot, old);
    }

    @Override
    public NonNullList<UnlimitedItemStack> copyToList() {
        return constructStackList(this.stacks.stream().map(UnlimitedItemStack::copy).toList());
    }

    @Override
    protected void setStacks(NonNullList<UnlimitedItemStack> stacks) {
        super.setStacks(constructStackList(stacks.stream().map(UnlimitedItemStack::copy).toList()));
        this.rebuildIndex();
    }

    protected void appendSlot() {
        NonNullList<UnlimitedItemStack> expanded = new NonNullList<>(new ArrayList<>(this.stacks), UnlimitedItemStack.EMPTY);
        expanded.add(UnlimitedItemStack.EMPTY);
        super.setStacks(expanded);
        this.updateIndex(expanded.size() - 1, UnlimitedItemStack.EMPTY);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        this.rebuildIndex();
    }

    /**
     * This handler has no type limit. Subclasses that expose a finite type limit can override this method.
     */
    public int getTypeLimit() {
        return Integer.MAX_VALUE;
    }

    public double getFullness() {
        int typeLimit = this.getTypeLimit();
        return typeLimit == Integer.MAX_VALUE ? 0.0 : (double) this.getTypeCount() / typeLimit;
    }

    public void sync(UnlimitedItemStacksResourceHandler items) {
        NonNullList<UnlimitedItemStack> source = UnlimitedItemStacksResourceHandler.trim(items.copyToList());
        NonNullList<UnlimitedItemStack> synced = NonNullList.withSize(this.size(), UnlimitedItemStack.EMPTY);
        for (int index = 0; index < Math.min(source.size(), synced.size()); index++) {
            synced.set(index, source.get(index));
        }
        this.setStacks(synced);
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
