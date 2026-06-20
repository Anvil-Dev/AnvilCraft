package dev.dubhe.anvilcraft.api.itemhandler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.network.util.BoolAndInt;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
import java.util.function.IntUnaryOperator;

@Getter(AccessLevel.PRIVATE)
public class TypeLimitItemStacksResourceHandler implements ResourceHandler<ItemResource>, ValueIOSerializable {
    public static final String STACKS_KEY = "stacks";
    public static final String TYPE_LIMIT_KEY = "type_limit";
    public static final String SPACE_SIZE_KEY = "space_size";
    public static final Codec<NonNullList<UnlimitedItemStack>> STACKS_CODEC = UnlimitedItemStack.OPTIONAL_CODEC
        .listOf()
        .xmap(TypeLimitItemStacksResourceHandler::constructStackList, TypeLimitItemStacksResourceHandler::trim);
    public static final MapCodec<TypeLimitItemStacksResourceHandler> CODEC = CodecUtil.mapCodec(
        TypeLimitItemStacksResourceHandler.STACKS_CODEC
            .fieldOf(TypeLimitItemStacksResourceHandler.STACKS_KEY)
            .forGetter(TypeLimitItemStacksResourceHandler::getStacks),
        Codec.INT
            .fieldOf(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY)
            .forGetter(TypeLimitItemStacksResourceHandler::getTypeLimit),
        Codec.INT
            .fieldOf(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY)
            .forGetter(TypeLimitItemStacksResourceHandler::getSpaceSize),
        TypeLimitItemStacksResourceHandler::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<UnlimitedItemStack>> STACKS_STREAM_CODEC = UnlimitedItemStack
        .STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(TypeLimitItemStacksResourceHandler::constructStackList, TypeLimitItemStacksResourceHandler::trim);
    public static final StreamCodec<RegistryFriendlyByteBuf, TypeLimitItemStacksResourceHandler> STREAM_CODEC = StreamCodec.composite(
        TypeLimitItemStacksResourceHandler.STACKS_STREAM_CODEC,
        TypeLimitItemStacksResourceHandler::getStacks,
        ByteBufCodecs.VAR_INT,
        TypeLimitItemStacksResourceHandler::getTypeLimit,
        ByteBufCodecs.VAR_INT,
        TypeLimitItemStacksResourceHandler::getSpaceSize,
        TypeLimitItemStacksResourceHandler::new
    );
    private int typeLimit;
    private int spaceSize;
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

    private TypeLimitItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks, int typeLimit, int spaceSize) {
        this.typeLimit = typeLimit;
        this.spaceSize = spaceSize;
        this.initStacks(stacks);
    }

    private void initStacks(NonNullList<UnlimitedItemStack> stacks) {
        this.space = 0;
        MAIN:
        for (UnlimitedItemStack stack : stacks) {
            // 剩余空间连一个都塞不下，直接下一个
            if (this.computeEmptySize(stack) < 1) {
                continue;
            }

            int space = TypeLimitItemStacksResourceHandler.computeSpace(stack, stack.count());

            // 塞得下整个栈，塞完下一个
            if (this.spaceSize == Integer.MAX_VALUE || this.space + space <= this.spaceSize) {
                this.space += space;
                for (int i = 0; i < this.stacks.size(); i++) {
                    UnlimitedItemStack exist = this.stacks.get(i);
                    if (exist.isSameItemSameComponents(stack)) {
                        UnlimitedItemStack original = exist.copy();
                        exist.setCount(stack.count());
                        this.onContentsChanged(i, original);
                        continue MAIN;
                    }
                }
                this.stacks.add(stack);
                continue;
            }

            // 塞不下整个栈，尝试找到能塞下的数量
            for (int i = stack.count() - 1; i >= 0; i--) {
                space = TypeLimitItemStacksResourceHandler.computeSpace(stack, i);
                // 找到了，塞完下一个
                if (this.space + space <= this.spaceSize) {
                    this.space += space;
                    for (int index = 0; index < this.stacks.size(); index++) {
                        UnlimitedItemStack exist = this.stacks.get(index);
                        if (exist.isSameItemSameComponents(stack)) {
                            UnlimitedItemStack original = exist.copy();
                            exist.setCount(stack.count());
                            this.onContentsChanged(index, original);
                            continue MAIN;
                        }
                    }
                    this.stacks.add(stack);
                    continue MAIN;
                }
            }
            // 找不到不塞，下一个
        }
    }

    public void addSpaceSize(IntUnaryOperator adder) {
        int spaceSize = adder.applyAsInt(this.spaceSize);
        if (spaceSize < this.spaceSize) {
            return;
        }
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

    public double getFullness() {
        return (double) this.space / this.spaceSize;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return TypeLimitItemStacksResourceHandler.computeSpace(resource, this.spaceSize);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    protected int computeEmptySize(ItemResource resource) {
        if (this.spaceSize == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return TypeLimitItemStacksResourceHandler.computeCount(resource, this.spaceSize - this.space);
    }

    protected int computeEmptySize(ItemInstance instance) {
        if (this.spaceSize == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return TypeLimitItemStacksResourceHandler.computeCount(instance, this.spaceSize - this.space);
    }

    protected int findNewSlot() {
        // 虽然种类限制为 Integer.MAX_VALUE 时视为无上限，但 Java 底层限制不允许 ArrayList 的大小超过 Integer.MAX_VALUE
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

        int index = this.findNewSlot();
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
        if (!stack.isEmpty() && !stack.isSameItemSameComponents(resource)) {
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
        if (!stack.isSameItemSameComponents(resource)) {
            return 0;
        }

        int count = stack.count();
        int extracted = Math.min(amount, count);
        if (extracted <= 0) {
            return 0;
        }

        this.snapshotJournals.get(index).updateSnapshots(transaction);
        this.stacks.set(index, new UnlimitedItemStack(resource, count - extracted));
        this.space -= TypeLimitItemStacksResourceHandler.computeSpace(resource, extracted);
        return extracted;
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

    public void sync(TypeLimitItemStacksResourceHandler items) {
        this.typeLimit = items.typeLimit;
        this.spaceSize = items.spaceSize;
        this.initStacks(items.stacks);
    }

    private static NonNullList<UnlimitedItemStack> trim(NonNullList<UnlimitedItemStack> list) {
        NonNullList<UnlimitedItemStack> result = TypeLimitItemStacksResourceHandler.constructStackList();
        for (UnlimitedItemStack stack : list) {
            if (stack.isEmpty()) {
                continue;
            }
            result.add(stack);
        }
        return result;
    }

    @Override
    public void serialize(ValueOutput output) {
        NonNullList<UnlimitedItemStack> saving = TypeLimitItemStacksResourceHandler.trim(this.stacks);
        output.store(TypeLimitItemStacksResourceHandler.STACKS_KEY, TypeLimitItemStacksResourceHandler.STACKS_CODEC, saving);
        output.putInt(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY, this.typeLimit);
        output.putInt(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY, this.spaceSize);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.typeLimit = input.getIntOr(TypeLimitItemStacksResourceHandler.TYPE_LIMIT_KEY, Integer.MAX_VALUE);
        input.getInt(TypeLimitItemStacksResourceHandler.SPACE_SIZE_KEY).ifPresent(size -> this.spaceSize = size);
        Optional<NonNullList<UnlimitedItemStack>> stacksOp = input.read(
            TypeLimitItemStacksResourceHandler.STACKS_KEY,
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
