package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.OverLimitItemHandler;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

@Getter
public class OverLimitItemContainerContents {
    private static final int NO_SLOT = -1;
    private static final int MAX_SIZE = 256;
    public static final OverLimitItemContainerContents EMPTY = new OverLimitItemContainerContents(NonNullList.create());
    public static final Codec<OverLimitItemContainerContents> CODEC = Slot.CODEC
        .sizeLimitedListOf(MAX_SIZE)
        .xmap(OverLimitItemContainerContents::fromSlots, OverLimitItemContainerContents::asSlots);
    public static final StreamCodec<RegistryFriendlyByteBuf, OverLimitItemContainerContents> STREAM_CODEC = UnlimitedItemStack
        .OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(MAX_SIZE))
        .map(OverLimitItemContainerContents::new, OverLimitItemContainerContents::getItems);
    private final NonNullList<UnlimitedItemStack> items;

    private OverLimitItemContainerContents(NonNullList<UnlimitedItemStack> items) {
        if (items.size() > MAX_SIZE) {
            throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is " + MAX_SIZE);
        } else {
            this.items = items;
        }
    }

    private OverLimitItemContainerContents(int size) {
        this(NonNullList.withSize(size, UnlimitedItemStack.EMPTY));
    }

    private OverLimitItemContainerContents(List<UnlimitedItemStack> items) {
        this(items.size());

        for (int i = 0; i < items.size(); i++) {
            this.items.set(i, items.get(i));
        }
    }

    private static OverLimitItemContainerContents fromSlots(List<Slot> slots) {
        OptionalInt maxSlot = slots.stream().mapToInt(Slot::index).max();
        if (maxSlot.isEmpty()) return EMPTY;
        OverLimitItemContainerContents contents = new OverLimitItemContainerContents(maxSlot.getAsInt() + 1);

        for (Slot slot : slots) {
            contents.items.set(slot.index(), slot.stack());
        }

        return contents;
    }

    public static OverLimitItemContainerContents fromItems(List<UnlimitedItemStack> items) {
        int i = findLastNonEmptySlot(items);
        if (i == NO_SLOT) return EMPTY;
        OverLimitItemContainerContents contents = new OverLimitItemContainerContents(i + 1);

        for (int j = 0; j <= i; j++) {
            contents.items.set(j, items.get(j).copy());
        }

        return contents;
    }

    public static OverLimitItemContainerContents fromItems(OverLimitItemHandler items) {
        int i = findLastNonEmptySlot(items);
        if (i == NO_SLOT) return EMPTY;
        OverLimitItemContainerContents contents = new OverLimitItemContainerContents(i + 1);

        for (int j = 0; j <= i; j++) {
            contents.items.set(j, items.getUnlimitedStackInSlot(j).copy());
        }

        return contents;
    }

    private static int findLastNonEmptySlot(List<UnlimitedItemStack> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) return i;
        }

        return NO_SLOT;
    }

    private static int findLastNonEmptySlot(OverLimitItemHandler items) {
        for (int i = items.getSlots() - 1; i >= 0; i--) {
            if (!items.getStackInSlot(i).isEmpty()) return i;
        }

        return NO_SLOT;
    }

    private List<Slot> asSlots() {
        List<Slot> list = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            UnlimitedItemStack stack = this.items.get(i);
            if (stack.isEmpty()) continue;
            list.add(new Slot(i, stack));
        }

        return list;
    }

    public void copyInto(NonNullList<UnlimitedItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            UnlimitedItemStack stack = i < this.items.size() ? this.items.get(i) : UnlimitedItemStack.EMPTY;
            list.set(i, stack.copy());
        }
    }

    public void copyInto(OverLimitItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            UnlimitedItemStack stack = i < this.items.size() ? this.items.get(i) : UnlimitedItemStack.EMPTY;
            handler.setUnlimitedStackInSlot(i, stack.copy());
        }
    }

    public UnlimitedItemStack copyOne() {
        return this.items.isEmpty() ? UnlimitedItemStack.EMPTY : this.items.getFirst().copy();
    }

    public Stream<UnlimitedItemStack> stream() {
        return this.items.stream().map(UnlimitedItemStack::copy);
    }

    public Stream<UnlimitedItemStack> nonEmptyStream() {
        return this.items.stream().filter(stack -> !stack.isEmpty()).map(UnlimitedItemStack::copy);
    }

    public Iterable<UnlimitedItemStack> nonEmptyItems() {
        return Iterables.filter(this.items, stack -> !stack.isEmpty());
    }

    public Iterable<UnlimitedItemStack> nonEmptyItemsCopy() {
        return Iterables.transform(this.nonEmptyItems(), UnlimitedItemStack::copy);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof OverLimitItemContainerContents contents
               && UnlimitedItemStack.listMatches(this.items, contents.items);
    }

    @Override
    public int hashCode() {
        return this.items.hashCode();
    }

    /**
     * Neo:
     * {@return the number of slots in this container}
     */
    public int getSlots() {
        return this.items.size();
    }

    /**
     * Neo: Gets a copy of the stack at a particular slot.
     *
     * @param slot The slot to check. Must be within [0, {@link #getSlots()}]
     *
     * @return A copy of the stack in that slot
     * @throws UnsupportedOperationException if the provided slot index is out-of-bounds.
     */
    public UnlimitedItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return this.items.get(slot).copy();
    }

    /**
     * Neo: Throws {@link UnsupportedOperationException} if the provided slot index is invalid.
     */
    private void validateSlotIndex(int slot) {
        if (slot >= 0 && slot < getSlots()) return;
        throw new UnsupportedOperationException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
    }

    record Slot(int index, UnlimitedItemStack stack) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.intRange(0, 255)
                .fieldOf("slot")
                .forGetter(Slot::index),
            UnlimitedItemStack.MAP_CODEC
                .forGetter(Slot::stack)
        ).apply(inst, Slot::new));
    }
}