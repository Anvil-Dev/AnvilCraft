package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 稀疏的无限堆叠物品处理器基类。
 *
 * <p>存储按「一种物品一个条目」组织：条目数等于种类数，故大型仓储可能有上千个条目。
 * 按物品种类做的事（找同种物品的条目、统计种类数、判断能否新增种类）若每次遍历全表，
 * 会在物品搬动路径上被反复调用而退化成条目数的平方级开销，因此这里维护一份
 * 「物品种类 → 条目下标」的索引，让这类查询回到 O(1)。</p>
 */
public class UnlimitedItemStacksResourceHandler implements IItemHandler, INBTSerializable<CompoundTag> {
    public static final String STACKS_KEY = "stacks";
    protected final NonNullList<UnlimitedItemStack> stacks;
    /** 物品种类（物品 + 数据组件）→ 存放该种类的条目下标；索引无效时为空 */
    private final Map<ResourceKey, Integer> typeIndex = new HashMap<>();
    /** 物品种类 → 该种类的现有总量（同一物品可能占多个条目） */
    private final Map<ResourceKey, Long> typeCounts = new HashMap<>();
    /** 索引是否与 {@link #stacks} 一致；条目内容变化时置否，下次查询种类时重建 */
    private boolean indexed;
    /** 索引中的非空条目数 */
    private int indexedEntryCount;

    public UnlimitedItemStacksResourceHandler(int size) {
        this.stacks = NonNullList.create();
        for (int index = 0; index < size; index++) {
            this.stacks.add(UnlimitedItemStack.EMPTY);
        }
    }

    public UnlimitedItemStacksResourceHandler(NonNullList<UnlimitedItemStack> stacks) {
        this.stacks = NonNullList.create();
        this.stacks.addAll(stacks);
    }

    public int size() {
        return this.stacks.size();
    }

    /**
     * 暴露给外部 {@code IItemHandler} 消费者的槽位数。
     *
     * <p>存储处理器只保留非空类型槽位（稀疏设计），但漏斗/管道等 {@code IItemHandler}
     * 消费者需要至少一个可插入/可追加的槽位。这里返回「现有非空类型数 + 1」的槽位，
     * 多出的一个作为「新类型」的增长槽。</p>
     *
     * <p>注意：必须基于 {@link #getTypeCount()}（非空类型数）而非 {@link #size()}（列表长度），
     * 否则在增长槽插入失败（空间/类型满）时会无限扩大槽位数，使
     * {@code ItemHandlerHelper.insertItemStacked} 的遍历循环永不终止。</p>
     */
    @Override
    public int getSlots() {
        return Math.min(this.getTypeCount() + 1, this.getTypeLimit());
    }

    public UnlimitedItemStack getUnlimitedStackInSlot(int index) {
        return this.stacks.get(index);
    }

    public long getAmountAsLong(int index) {
        return this.stacks.get(index).getCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= this.stacks.size()) {
            return ItemStack.EMPTY;
        }
        return this.stacks.get(slot).toStack();
    }

    /**
     * 把存储扩展到至少包含 {@code slot} 槽位（追加空槽）。
     */
    protected void ensureSlot(int slot) {
        while (this.stacks.size() <= slot) {
            this.stacks.add(UnlimitedItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!this.isItemValid(slot, stack)) return stack;
        if (slot >= this.stacks.size()) {
            if (slot >= this.getSlots()) {
                return stack;
            }
            if (simulate) {
                // 增长槽：无容量上限的基类接受全部
                return ItemStack.EMPTY;
            }
            this.ensureSlot(slot);
        }
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
            this.invalidateTypeIndex();
            this.onContentsChanged(slot, existing);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 尝试把物品堆插入到任意匹配或空槽位中，返回未能插入的剩余物品堆。
     */
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int slot = 0; slot < this.size(); slot++) {
            UnlimitedItemStack existing = this.stacks.get(slot);
            if (existing.isEmpty() || existing.isSameItemSameComponents(stack)) {
                stack = this.insertItem(slot, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        if (slot < 0 || slot >= this.stacks.size()) {
            return ItemStack.EMPTY;
        }

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
            this.invalidateTypeIndex();
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
            this.invalidateTypeIndex();
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
        this.ensureTypeIndex();
        return this.indexedEntryCount;
    }

    /**
     * 查找存放指定物品种类的条目下标，没有则返回 {@code -1}。
     *
     * <p>供按种类取放物品的调用方使用（如仓储端口按标记物品找核心里的条目）；
     * 直接遍历全表在上千种类的仓储里代价极高。</p>
     */
    public int findSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        this.ensureTypeIndex();
        Integer slot = this.typeIndex.get(ResourceKey.of(stack));
        // 键可能与桶里已有的条目（哈希碰撞、或条目已被换成别的种类）不符，届时以条目为准
        return slot != null && this.stacks.get(slot).isSameItemSameComponents(stack) ? slot : -1;
    }

    /** 按种类数统计当前种类数；用于同一物品可能占多个条目的子类。 */
    protected int countDistinctTypes() {
        this.ensureTypeIndex();
        return this.typeIndex.size();
    }

    /**
     * 存储中该物品种类的现有总量；不存在时为 0。
     *
     * <p>按种类算容量上限时要扣掉已有量，而这不值得为此再遍历一遍全表。</p>
     */
    protected long countOfType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        this.ensureTypeIndex();
        Long count = this.typeCounts.get(ResourceKey.of(stack));
        return count == null ? 0 : count;
    }

    /** 重建种类索引，使其与当前条目内容一致。 */
    private void ensureTypeIndex() {
        if (this.indexed) {
            return;
        }
        this.typeIndex.clear();
        this.typeCounts.clear();
        int entries = 0;
        for (int slot = 0; slot < this.stacks.size(); slot++) {
            UnlimitedItemStack stack = this.stacks.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            entries++;
            ItemStack item = stack.toStack();
            ResourceKey key = ResourceKey.of(item);
            this.typeIndex.putIfAbsent(key, slot);
            this.typeCounts.merge(key, (long) stack.getCount(), Long::sum);
        }
        this.indexedEntryCount = entries;
        this.indexed = true;
    }

    /** 标记种类索引失效；条目内容被改动后必须调用。 */
    protected void invalidateTypeIndex() {
        this.indexed = false;
        this.indexedEntryCount = 0;
    }

    /**
     * This handler has no type limit. Subclasses that expose a finite type limit can override this method.
     */
    public int getTypeLimit() {
        return Integer.MAX_VALUE;
    }

    /**
     * 无限空间存储的饱满度：按「已用类型数 / 类型上限」计算。
     *
     * <p>不能按 count/maxStack 累加——无限空间下空间占用率恒为 0，而低堆叠上限物品
     * （如雪球 maxStack 16）多槽累加会超过 1，导致容量条溢出。</p>
     */
    public double getFullness() {
        int typeLimit = this.getTypeLimit();
        if (typeLimit == Integer.MAX_VALUE) {
            return 0.0;
        }
        return (double) this.getTypeCount() / typeLimit;
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
        this.invalidateTypeIndex();
        this.onContentsChanged(-1, UnlimitedItemStack.EMPTY);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag items = new ListTag();
        for (int index = 0; index < this.stacks.size(); index++) {
            UnlimitedItemStack stack = this.stacks.get(index);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", index);
            entry.put("Stack", UnlimitedItemStack.CODEC
                .encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow());
            items.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        NonNullList<UnlimitedItemStack> loaded = NonNullList.create();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt("Slot");
            if (entry.contains("Stack", Tag.TAG_COMPOUND)) {
                UnlimitedItemStack stack = UnlimitedItemStack.CODEC
                    .parse(provider.createSerializationContext(NbtOps.INSTANCE), entry.get("Stack"))
                    .result()
                    .orElse(UnlimitedItemStack.EMPTY);
                while (loaded.size() <= slot) {
                    loaded.add(UnlimitedItemStack.EMPTY);
                }
                loaded.set(slot, stack);
            }
        }
        this.stacks.clear();
        this.stacks.addAll(loaded);
        this.invalidateTypeIndex();
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
        NonNullList<UnlimitedItemStack> result = NonNullList.create();
        result.addAll(from);
        return result;
    }

    protected static NonNullList<UnlimitedItemStack> trim(List<UnlimitedItemStack> from) {
        NonNullList<UnlimitedItemStack> result = NonNullList.create();
        for (UnlimitedItemStack stack : from) {
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     * 以物品+数据组件为相等的键（忽略数量）。{@link ItemStack} 未重写
     * {@code equals}/{@code hashCode}，直接用 {@code Map<ItemStack,...>} 或
     * {@code Set<ItemStack>} 做合并/去重键会退化为引用相等，无法合并同种物品。
     */
    public record ResourceKey(ItemStack stack) {
        public static ResourceKey of(ItemStack stack) {
            return new ResourceKey(stack.copyWithCount(1));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ResourceKey(ItemStack stack1)
                   && ItemStack.isSameItemSameComponents(this.stack, stack1);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }
    }
}
