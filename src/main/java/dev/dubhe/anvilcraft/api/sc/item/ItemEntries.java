package dev.dubhe.anvilcraft.api.sc.item;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrades;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        IntList stackIndexes = this.entries.get(item);
        if (stackIndexes == null) {
            if (this.isMaxEntries()) return stack.getCount();
            int stackIndex = this.allocateStackIndex(stack);
            this.entries.put(item, IntArrayList.of(stackIndex));
        } else {
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
            this.stacks.set(index, UnlimitedItemStack.EMPTY);
            return stack.copy();
        }
        return stack.splitUnlimited(amount);
    }

    private int allocateStackIndex(ItemStack stack) {
        List<UnlimitedItemStack> stacks = this.stacks;
        for (int i = 0, stacksSize = stacks.size(); i < stacksSize; i++) {
            UnlimitedItemStack storedStack = stacks.get(i);
            if (storedStack.isEmpty()) {
                this.stacks.set(i, new UnlimitedItemStack(stack));
                return i;
            }
        }

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

    public @Unmodifiable List<OrderPos> getOrder(
        Predicate<UnlimitedItemStack> filter,
        Comparator<UnlimitedItemStack> sorter,
        boolean shouldFoldNbt
    ) {
        record OrderEntry(UnlimitedItemStack stack, OrderPos.Mutable posMut) {
        }

        List<OrderEntry> entries = new ArrayList<>();
        Set<Item> folded = new HashSet<>();
        for (int i = 0; i < this.stacks.size(); i++) {
            UnlimitedItemStack stack = this.stacks.get(i);
            if (stack.isEmpty() || !filter.test(stack)) continue;
            if (!shouldFoldNbt) {
                entries.add(new OrderEntry(stack, new OrderPos.Mutable(i)));
                continue;
            }
            var item = stack.getItem();
            if (folded.contains(item)) {
                for (var entry : entries) {
                    if (entry.stack().is(item)) entry.posMut().folded(true);
                    break;
                }
            } else {
                folded.add(item);
                entries.add(new OrderEntry(stack, new OrderPos.Mutable(i)));
            }
        }
        entries.sort(Comparator.comparing(OrderEntry::stack, sorter));
        List<OrderPos> order = new ArrayList<>();
        for (OrderEntry entry : entries) {
            order.add(entry.posMut().toImmutable());
        }
        return Collections.unmodifiableList(order);
    }

    public void sync(ItemEntries entries) {
        this.clear();
        this.stacks.addAll(entries.stacks);
        this.entries.putAll(entries.entries);
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
