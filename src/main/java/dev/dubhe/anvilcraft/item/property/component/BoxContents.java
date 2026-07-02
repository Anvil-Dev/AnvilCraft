package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.amulet.AmuletBoxItem;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntFunction;

@SuppressWarnings("unused")
public record BoxContents(List<ItemStack> amulets, List<ItemStack> totems, int selection, int usage) implements TooltipComponent {
    public static final int CAPACITY = 16;
    public static final BoxContents EMPTY = new BoxContents(List.of(), List.of(), 0);
    public static final Codec<BoxContents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        ItemStack.CODEC.listOf().fieldOf("amulets").forGetter(BoxContents::amulets),
        ItemStack.CODEC.listOf().fieldOf("totems").forGetter(BoxContents::totems),
        Codec.INT.fieldOf("selection").forGetter(BoxContents::selection)
    ).apply(ins, BoxContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoxContents> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), BoxContents::amulets,
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), BoxContents::totems,
        ByteBufCodecs.INT, BoxContents::selection,
        BoxContents::new
    );

    BoxContents(List<ItemStack> amulets, List<ItemStack> totems, int selectedItemIndex) {
        this(amulets, totems, selectedItemIndex, computeUsage(amulets, totems));
    }

    public static int sum(List<ItemStack> amulets, List<ItemStack> totems, ToIntFunction<ItemStack> fn) {
        int sum = totems.size();
        for (ItemStack it : amulets) {
            int i = fn.applyAsInt(it);
            sum += i;
        }
        return sum;
    }

    public static int computeUsage(List<ItemStack> amulets, List<ItemStack> totems) {
        return BoxContents.sum(
            amulets,
            totems,
            it -> it.has(ModComponents.AMULET) ? Objects.requireNonNull(it.get(ModComponents.AMULET)).getWeight() : 0
        );
    }

    public @Unmodifiable List<ItemStack> allItems() {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        builder.addAll(this.amulets);
        builder.addAll(this.totems);
        return builder.build();
    }

    public Mutable mutable() {
        return new Mutable(this);
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

    public static class Mutable {
        private final List<ItemStack> amulets;
        private final List<ItemStack> totems;
        private int selection;
        private int usage;

        Mutable(BoxContents contents) {
            this.amulets = new ArrayList<>(contents.amulets);
            this.totems = new ArrayList<>(contents.totems);
            this.usage = BoxContents.computeUsage(contents.amulets, contents.totems);
            this.selection = contents.selection;
        }

        public Optional<ItemStack> tryInsert(ItemStack stack) {
            if (stack.isEmpty()) return Optional.of(ItemStack.EMPTY);
            if (stack.has(ModComponents.AMULET)) {
                IAmulet amulet = stack.get(ModComponents.AMULET);
                if (this.usage + Objects.requireNonNull(amulet).getWeight() > BoxContents.CAPACITY) return Optional.empty();
                for (ItemStack exist : this.amulets) {
                    if (Objects.requireNonNull(exist.get(ModComponents.AMULET)).getWeight() > 6) return Optional.empty();
                }
                this.usage += amulet.getWeight();
                this.amulets.add(stack.split(1));
                return Optional.of(stack);
            } else if (stack.is(ModItemTags.TOTEM)) {
                if (this.usage + 1 > BoxContents.CAPACITY) return Optional.empty();
                this.usage++;
                this.totems.add(stack.split(1));
                return Optional.of(stack);
            }
            return Optional.empty();
        }

        public ItemStack pop() {
            ItemStack stack = ItemStack.EMPTY;

            if (this.amulets.size() > this.selection) {
                stack = this.amulets.remove(this.selection);
                if (stack.has(ModComponents.AMULET)) {
                    this.usage -= Objects.requireNonNull(stack.get(ModComponents.AMULET)).getWeight();
                }
            } else if (this.totems.size() > this.selection - this.amulets.size()) {
                stack = this.totems.remove(this.selection - this.amulets.size());
                if (stack.is(ModItemTags.TOTEM)) {
                    this.usage--;
                }
            }

            this.usage = Math.clamp(this.usage, 0, BoxContents.CAPACITY);
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
