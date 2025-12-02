package dev.dubhe.anvilcraft.api.container.item;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMaps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemEntries implements Iterable<UnlimitedItemStack> {
    public static final MapCodec<ItemEntries> CODEC = ItemEntriesSerialization.CODEC.xmap(
        ItemEntriesSerialization::toEntries,
        ItemEntriesSerialization::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntries> STREAM_CODEC = ItemEntriesSerialization.STREAM_CODEC.map(
        ItemEntriesSerialization::toEntries,
        ItemEntriesSerialization::new
    );

    final List<UnlimitedItemStack> stacks;
    final Map<Holder<Item>, IntList> entries;
    private Upgrades upgrades;

    public ItemEntries(Upgrades upgrades) {
        this(upgrades, new ArrayList<>(), new HashMap<>());
    }

    private ItemEntries(Upgrades upgrades, List<UnlimitedItemStack> stacks, Map<Holder<Item>, IntList> entries) {
        this.upgrades = upgrades;
        this.stacks = stacks;
        this.entries = entries;
    }

    ItemEntries(List<UnlimitedItemStack> stacks, Map<Holder<Item>, IntList> entries) {
        this.stacks = stacks;
        this.entries = entries;
    }

    public boolean isMaxEntries() {
        return this.entries.size() >= this.upgrades.getEntryLimit();
    }

    public UnlimitedItemStack getItem(int index) {
        if (index < 0 || index >= this.stacks.size()) return UnlimitedItemStack.EMPTY;
        return this.stacks.get(index);
    }

    public int addItem(ItemStack stack) {
        Holder<Item> item = stack.getItemHolder();
        if (!this.entries.containsKey(item)) {
            if (this.isMaxEntries()) return stack.getCount();
            int stackIndex = this.allocateStackIndex(stack);
            this.entries.put(item, IntArrayList.of(stackIndex));
        } else {
            IntList stackIndexes = this.entries.get(item);
            for (int stackIndex : stackIndexes) {
                UnlimitedItemStack storedStack = this.stacks.get(stackIndex);
                if (!storedStack.isSameItemSameComponents(stack)) continue;
                int maxCount = this.upgrades.getMaxStackSize(stack);
                if (storedStack.getCount() + stack.getCount() > maxCount) {
                    int remain = stack.getCount() - (maxCount - storedStack.getCount());
                    storedStack.setCount(maxCount);
                    return remain;
                }
                storedStack.grow(stack.getCount());
                return 0;
            }
            int stackIndex = this.allocateStackIndex(stack);
            stackIndexes.add(stackIndex);
        }
        return 0;
    }

    public UnlimitedItemStack split(int index, int amount) {
        UnlimitedItemStack stack = this.stacks.get(index);
        if (amount >= stack.getCount()) {
            Holder<Item> item = stack.getStack().getItemHolder();
            this.entries.get(item).removeIf(i -> i == index);
            this.stacks.remove(index);
            return stack.copy();
        }
        return stack.splitUnlimited(amount);
    }

    private int allocateStackIndex(ItemStack stack) {
        int stackIndex = this.stacks.size();
        this.stacks.add(stackIndex, new UnlimitedItemStack(stack));
        return stackIndex;
    }

    public int entrySize() {
        return this.entries.size();
    }

    public int stackSize() {
        return this.stacks.size();
    }

    public Int2BooleanMap getOrder(
        Predicate<UnlimitedItemStack> filter,
        Comparator<UnlimitedItemStack> sorter,
        boolean shouldFoldNbt
    ) {
        record OrderEntry(UnlimitedItemStack stack, int originalOrder, boolean folded) {
        }

        List<OrderEntry> entries = new ArrayList<>();
        Object2IntMap<Item> folded = new Object2IntArrayMap<>();
        for (int i = 0; i < this.stacks.size(); i++) {
            UnlimitedItemStack stack = this.stacks.get(i);
            if (!filter.test(stack)) continue;
            if (shouldFoldNbt) {
                if (folded.containsKey(stack.getItem())) {
                    entries.set(folded.getInt(stack.getItem()), new OrderEntry(stack, i, true));
                    continue;
                }
                int index = entries.size();
                folded.put(stack.getItem(), index);
                entries.add(index, new OrderEntry(stack, i, false));
                continue;
            }
            entries.add(new OrderEntry(stack, i, false));
        }
        entries.sort(Comparator.comparing(OrderEntry::stack, sorter));
        Int2BooleanMap order = new Int2BooleanArrayMap();
        for (OrderEntry pair : entries) {
            order.put(pair.originalOrder, pair.folded);
        }
        return Int2BooleanMaps.unmodifiable(order);
    }

    public void sync(ItemEntries entries, Upgrades upgrades) {
        this.stacks.addAll(entries.stacks);
        this.entries.putAll(entries.entries);
        this.upgrades = upgrades;
    }

    public void clear() {
        this.stacks.clear();
        this.entries.clear();
    }

    @Override
    public @NotNull Iterator<UnlimitedItemStack> iterator() {
        return this.stacks.iterator();
    }

    @Override
    public void forEach(Consumer<? super UnlimitedItemStack> action) {
        this.stacks.forEach(action);
    }

    @Override
    public Spliterator<UnlimitedItemStack> spliterator() {
        return this.stacks.spliterator();
    }

    public int getFirstIndexForItem(Holder<Item> item) {
        return ListUtil.safelyGet(this.entries.get(item), 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stacks, this.entries);
    }
}
