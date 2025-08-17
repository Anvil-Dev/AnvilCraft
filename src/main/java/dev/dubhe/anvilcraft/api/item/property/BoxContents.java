package dev.dubhe.anvilcraft.api.item.property;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.item.amulet.AmuletBoxItem;
import dev.dubhe.anvilcraft.item.amulet.AmuletItem;
import dev.dubhe.anvilcraft.util.ListUtil;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

@SuppressWarnings("unused")
public final class BoxContents implements TooltipComponent {
    public static final BoxContents EMPTY = new BoxContents(List.of(), List.of(), 0);
    public static final Codec<BoxContents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        ItemStack.CODEC.listOf().fieldOf("amulets").forGetter(BoxContents::getAmulets),
        ItemStack.CODEC.listOf().fieldOf("totems").forGetter(BoxContents::getTotems),
        Codec.INT.fieldOf("selection").forGetter(BoxContents::getSelection)
    ).apply(ins, BoxContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoxContents> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), BoxContents::getAmulets,
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), BoxContents::getTotems,
        ByteBufCodecs.INT, BoxContents::getSelection,
        BoxContents::new
    );
    @Getter
    private final List<ItemStack> amulets;
    @Getter
    private final List<ItemStack> totems;
    @Getter
    private final int selection;
    @Getter
    private final int usage;

    BoxContents(List<ItemStack> amulets, List<ItemStack> totems, int selectedItemIndex) {
        this.amulets = amulets;
        this.totems = totems;
        this.selection = selectedItemIndex;
        this.usage = computeUsage();
    }

    BoxContents(List<ItemStack> amulets, List<ItemStack> totems, int selectedItemIndex, int computedUsage) {
        this.amulets = amulets;
        this.totems = totems;
        this.selection = selectedItemIndex;
        this.usage = computedUsage;
    }

    public int sum(ToIntFunction<ItemStack> fn) {
        int sum = this.totems.size();
        for (ItemStack it : this.amulets) {
            int i = fn.applyAsInt(it);
            sum += i;
        }
        return sum;
    }

    int computeUsage() {
        return sum(it -> it.getItem() instanceof AmuletItem amulet ? amulet.getWeight() : 0);
    }

    public List<ItemStack> allItems() {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        builder.addAll(this.amulets);
        builder.addAll(this.totems);
        return builder.build();
    }

    public BoxContents.Mutable mutable() {
        return new Mutable(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BoxContents that)) return false;
        return this.selection == that.selection
               && this.usage == that.usage
               && ListUtil.equals(this.amulets, that.amulets, ItemStack::isSameItemSameComponents)
               && ListUtil.equals(this.totems, that.totems, ItemStack::isSameItemSameComponents);
    }

    public boolean isEmpty() {
        return this.usage <= 0;
    }

    public boolean isAmuletEmpty() {
        return this.usage >= 0 && this.amulets.isEmpty() && !this.totems.isEmpty();
    }

    public int getMaxSelection() {
        return this.amulets.size() + this.totems.size(); // this makes sense
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.amulets, this.totems, this.selection, this.usage);
    }

    public static class Mutable {
        private final List<ItemStack> amulets;
        private final List<ItemStack> totems;
        private int selection;
        private int usage;

        Mutable(BoxContents contents) {
            this.amulets = new ArrayList<>(contents.amulets);
            this.totems = new ArrayList<>(contents.totems);
            this.usage = contents.computeUsage();
            this.selection = contents.selection;
        }

        public boolean tryInsert(ItemStack itemStack) {
            if (itemStack.getItem() instanceof AmuletItem item) {
                if (this.usage + item.getWeight() > AmuletBoxItem.CAPACITY) return false;
                this.usage += item.getWeight();
                this.amulets.add(itemStack.copy());
                return true;
            } else if (itemStack.is(ModItemTags.TOTEM)) {
                if (this.usage + 1 > AmuletBoxItem.CAPACITY) return false;
                this.usage++;
                this.totems.add(itemStack.copy());
                return true;
            }
            return false;
        }

        public ItemStack pop() {
            ItemStack stack = ItemStack.EMPTY;

            if (this.amulets.size() > this.selection) {
                stack = this.amulets.remove(this.selection);
                if (stack.getItem() instanceof AmuletItem item) {
                    this.usage -= item.getWeight();
                }
            } else if (this.totems.size() > this.selection - this.amulets.size()) {
                stack = this.totems.remove(this.selection - this.amulets.size());
                if (stack.is(ModItemTags.TOTEM)) {
                    this.usage--;
                }
            }

            this.usage = Math.clamp(this.usage, 0, AmuletBoxItem.CAPACITY);
            return stack.copy();
        }

        public void select(int selection) {
            this.selection = selection;
        }

        public BoxContents immutable() {
            return new BoxContents(ImmutableList.copyOf(this.amulets), ImmutableList.copyOf(this.totems), this.selection);
        }

        public ItemStack popTotem() {
            if (this.totems.isEmpty()) return ItemStack.EMPTY;
            ItemStack first = this.totems.removeFirst();
            if (first.is(ModItemTags.TOTEM)) {
                this.usage--;
            }
            this.usage = Math.clamp(this.usage, 0, AmuletBoxItem.CAPACITY);
            return first;
        }
    }
}
