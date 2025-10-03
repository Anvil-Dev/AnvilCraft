package dev.dubhe.anvilcraft.api.container.item;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.Getter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ItemEntries extends AbstractList<UnlimitedItemStack> {
    public static final Codec<ItemEntries> CODEC = ItemEntry.CODEC.listOf()
        .xmap(ArrayList::new, Function.identity())
        .xmap(ItemEntries::new, ItemEntries::getEntries);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntries> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, ItemEntry.STREAM_CODEC),
        ItemEntries::getEntries,
        ItemEntries::new
    );
    private Upgrades upgrades;
    @Getter
    private final ArrayList<ItemEntry> entries;
    private int size;

    public ItemEntries(Upgrades upgrades) {
        this(upgrades, new ArrayList<>());
    }

    public ItemEntries(Upgrades upgrades, ArrayList<ItemEntry> entries) {
        this.upgrades = upgrades;
        this.entries = entries;
        this.calculateSize();
    }

    private ItemEntries(ArrayList<ItemEntry> entries) {
        this.entries = new ArrayList<>(entries);
        this.calculateSize();
    }

    public void syncData(Upgrades upgrades) {
        this.upgrades = upgrades;
    }

    public void setChanged() {
        this.calculateSize();
    }

    private void calculateSize() {
        this.size = 0;
        for (ItemEntry entry : this.getEntries()) {
            this.size += entry.toStacks().size();
        }
    }

    @Override
    public UnlimitedItemStack get(int index) {
        this.modCount++;
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.size());
        }
        int original = index;
        for (ItemEntry entry : this.getEntries()) {
            List<UnlimitedItemStack> stacks = entry.toStacks();
            if (index >= stacks.size()) {
                index -= stacks.size();
                continue;
            }
            return stacks.get(index);
        }
        throw new IndexOutOfBoundsException("Index " + original + " out of bounds for length " + this.size());
    }

    public UnlimitedItemStack removeCount(int index, int count) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.size());
        }
        int original = index;
        for (Iterator<ItemEntry> iterator = this.getEntries().iterator(); iterator.hasNext(); ) {
            ItemEntry entry = iterator.next();
            List<UnlimitedItemStack> stacks = entry.toStacks();
            if (index >= stacks.size()) {
                index -= stacks.size();
                continue;
            }
            UnlimitedItemStack data = stacks.get(index);
            ItemEntry.ModifyResult result = entry.modifyCount(data.getStack(), this.upgrades.getStackPower(), old -> old - count);
            boolean modded = false;
            if (!result.stackCountChanged().isDefault()) {
                modded = true;
                this.calculateSize();
            }
            if (result.result().isFalse()) {
                iterator.remove();
                modded = true;
                this.calculateSize();
            }
            if (modded) this.modCount++;
            return data.copyWithCount(count);
        }
        throw new IndexOutOfBoundsException("Index " + original + " out of bounds for length " + this.size());
    }

    @Override
    public UnlimitedItemStack set(int index, UnlimitedItemStack element) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.size());
        }
        int original = index;
        for (Iterator<ItemEntry> iterator = this.getEntries().iterator(); iterator.hasNext(); ) {
            ItemEntry entry = iterator.next();
            List<UnlimitedItemStack> stacks = entry.toStacks();
            if (index >= stacks.size()) {
                index -= stacks.size();
                continue;
            }
            int delta = element.getCount();
            ItemEntry.ModifyResult result = entry.modifyCount(element.getStack(), this.upgrades.getStackPower(), old -> old - delta);
            boolean modded = false;
            if (!result.stackCountChanged().isDefault()) {
                modded = true;
                this.calculateSize();
            }
            if (result.result().isFalse()) {
                iterator.remove();
                modded = true;
                this.calculateSize();
            }
            if (modded) this.modCount++;
            return stacks.getFirst().copyWithCount(result.oldCount());
        }
        throw new IndexOutOfBoundsException("Index " + original + " out of bounds for length " + this.size());
    }

    public boolean add(ItemStack stack) {
        DataComponentPatch patch = stack.getComponentsPatch();
        for (ItemEntry entry : this.getEntries()) {
            for (ItemEntry.EntryData data : entry.data()) {
                if (data.getPatch().equals(patch)) {
                    return entry.merge(stack, this.upgrades.getStackPower()).result().isTrue();
                }
            }
        }
        this.modCount++;
        if (this.upgrades.getEntryLimit() == this.getEntries().size()) return false;
        this.getEntries().add(ItemEntry.of(stack.copy()));
        stack.setCount(0);
        this.calculateSize();
        return true;
    }

    @Override
    public boolean add(UnlimitedItemStack stack) {
        DataComponentPatch patch = stack.getStack().getComponentsPatch();
        for (ItemEntry entry : this.getEntries()) {
            for (ItemEntry.EntryData data : entry.data()) {
                if (data.getPatch().equals(patch)) {
                    return entry.merge(stack, this.upgrades.getStackPower()).result().isTrue();
                }
            }
        }
        this.modCount++;
        if (this.upgrades.getEntryLimit() == this.getEntries().size()) return false;
        this.getEntries().add(ItemEntry.of(stack.copy()));
        stack.setCount(0);
        this.calculateSize();
        return true;
    }

    @Override
    public void add(int index, UnlimitedItemStack element) {
        if (index < 0 || index > this.size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
        }
        int original = index;
        for (ItemEntry entry : this.getEntries()) {
            List<UnlimitedItemStack> stacks = entry.toStacks();
            if (index >= stacks.size()) {
                index -= stacks.size();
                continue;
            }
            if (entry.merge(element, this.upgrades.getStackPower()).result().isFalse()) {
                this.getEntries().add(ItemEntry.of(element));
                this.modCount++;
            }
        }
        throw new IndexOutOfBoundsException("Index: " + original + ", Size: " + this.size());
    }

    @Override
    public UnlimitedItemStack remove(int index) {
        this.modCount++;
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.size());
        }
        int original = index;
        for (Iterator<ItemEntry> iterator = this.getEntries().iterator(); iterator.hasNext(); ) {
            ItemEntry entry = iterator.next();
            List<UnlimitedItemStack> stacks = entry.toStacks();
            if (index >= stacks.size()) {
                index -= stacks.size();
                continue;
            }
            UnlimitedItemStack forRemoval = stacks.get(index);
            ItemEntry.ModifyResult result = entry.modifyCount(forRemoval.getStack(), this.upgrades.getStackPower(), old -> 0);
            if (result.result().isDefault()) {
                iterator.remove();
                this.calculateSize();
            }
            return forRemoval;
        }
        throw new IndexOutOfBoundsException("Index " + original + " out of bounds for length " + this.size());
    }

    @Override
    public int size() {
        return this.size;
    }

    public boolean isMaxEntries() {
        return this.entries.size() >= this.upgrades.getEntryLimit();
    }

    @Override
    public Iterator<UnlimitedItemStack> iterator() {
        return new Itr();
    }

    @Override
    public ListIterator<UnlimitedItemStack> listIterator() {
        return new ListItr();
    }

    private class Itr implements Iterator<UnlimitedItemStack> {
        int entryCursor, stackCursor, totalCursor;
        int lastRet = -1;
        int expectedModCount = ItemEntries.this.modCount;

        private Itr() {
        }

        @Override
        public boolean hasNext() {
            return this.entryCursor < ItemEntries.this.getEntries().size();
        }

        @Override
        public UnlimitedItemStack next() {
            this.checkForComodification();
            List<UnlimitedItemStack> stacks = ItemEntries.this.getEntries().get(this.entryCursor).toStacks();
            while (stacks.size() == this.stackCursor + 1) {
                this.entryCursor++;
                if (this.entryCursor == ItemEntries.this.getEntries().size()) throw new NoSuchElementException();
                stacks = ItemEntries.this.getEntries().get(this.entryCursor).toStacks();
                this.stackCursor = 0;
            }
            UnlimitedItemStack result = stacks.get(this.stackCursor);
            this.stackCursor++;
            this.totalCursor++;
            this.lastRet = this.totalCursor;
            return result;
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) throw new IllegalStateException();
            this.checkForComodification();

            try {
                ItemEntries.this.remove(this.lastRet);
                this.totalCursor = this.lastRet;
                this.lastRet = -1;
                this.expectedModCount = ItemEntries.this.modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super UnlimitedItemStack> action) {
            Objects.requireNonNull(action);
            final int size = ItemEntries.this.size;
            int i = this.totalCursor;
            if (i >= size) return;
            for (ItemEntry entry : ItemEntries.this.getEntries()) {
                for (UnlimitedItemStack stack : entry.toStacks()) {
                    if (ItemEntries.this.modCount == this.expectedModCount) {
                        action.accept(stack);
                    }
                }
            }
            // 在最后更新一次以减少堆写入流量
            this.totalCursor = i;
            this.lastRet = i - 1;
            this.checkForComodification();
        }

        final void checkForComodification() {
            if (ItemEntries.this.modCount != this.expectedModCount) throw new ConcurrentModificationException();
        }
    }

    private class ListItr extends Itr implements ListIterator<UnlimitedItemStack> {
        private ListItr() {
        }

        @Override
        public boolean hasPrevious() {
            return this.entryCursor > 0 || this.stackCursor > 0;
        }

        @Override
        public int nextIndex() {
            return this.totalCursor;
        }

        @Override
        public int previousIndex() {
            return this.totalCursor - 1;
        }

        @Override
        public UnlimitedItemStack previous() {
            this.checkForComodification();
            List<UnlimitedItemStack> stacks = ItemEntries.this.getEntries().get(this.entryCursor).toStacks();
            while (this.stackCursor == 0) {
                this.entryCursor--;
                if (this.entryCursor < 0) throw new NoSuchElementException();
                stacks = ItemEntries.this.getEntries().get(this.entryCursor).toStacks();
                this.stackCursor = stacks.size() - 1;
            }
            UnlimitedItemStack result = stacks.get(this.stackCursor);
            this.stackCursor--;
            this.totalCursor--;
            return result;
        }

        @Override
        public void set(UnlimitedItemStack stack) {
            if (this.lastRet < 0) throw new IllegalStateException();
            this.checkForComodification();

            try {
                ItemEntry entry = ItemEntries.this.getEntries().get(this.entryCursor);
                List<UnlimitedItemStack> stacks = entry.toStacks();
                UnlimitedItemStack data = stacks.get(this.stackCursor);
                int delta = data.getCount() - stack.getCount();
                ItemEntry.ModifyResult result = entry.modifyCount(
                    data.getStack(),
                    ItemEntries.this.upgrades.getStackPower(),
                    old -> old - delta
                );
                if (!result.stackCountChanged().isTrue()) return;
                this.totalCursor++;
                this.lastRet = -1;
                this.expectedModCount = ++ItemEntries.this.modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void add(UnlimitedItemStack stack) {
            this.checkForComodification();

            try {
                int i = this.totalCursor;
                ItemEntries.this.add(i, stack);
                this.totalCursor = i + 1;
                this.lastRet = -1;
                this.expectedModCount = ItemEntries.this.modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
