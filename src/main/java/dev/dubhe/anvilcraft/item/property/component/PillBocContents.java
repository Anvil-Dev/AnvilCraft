package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.item.PillItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public record PillBocContents(int index, List<ItemStack> pills) {
    public static final PillBocContents EMPTY = new PillBocContents(ImmutableList.of());

    public static final Codec<PillBocContents> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        ItemStack.CODEC.listOf().fieldOf("pills").forGetter(PillBocContents::pills)
    ).apply(instance, PillBocContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PillBocContents> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PillBocContents::pills,
        PillBocContents::new
    );

    public PillBocContents(List<ItemStack> pills) {
        this(-1, pills);
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    public static class Mutable {
        @Getter
        private int index;
        private List<ItemStack> pills;

        public Mutable(PillBocContents contents) {
            this.index = contents.index;
            this.pills = new ObjectArrayList<>(contents.pills);
        }

        public void setDefaultIndex() {
            this.index = -1;
        }

        public void setIndex(int index) {
            if (index > this.pills.size() - 1) {
                this.index = 0;
            } else if (index < 0) {
                this.index = this.pills.size() - 1;
            } else {
                this.index = index;
            }
        }

        public boolean insert(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return false;
            }
            if (itemStack.is(ModFoodItems.PILL) && this.pills.size() < 8) {
                for (int i = 0; i < this.pills.size(); i++) {
                    ItemStack pill = this.pills.get(i);
                    if (pill.getCount() >= 64) {
                        continue;
                    }
                    if (ItemStack.isSameItemSameComponents(pill, itemStack)) {
                        int remain = itemStack.getCount() - (64 - pill.getCount());
                        if (remain > 0) {
                            itemStack.setCount(remain);
                            this.pills.add(itemStack);
                            pill.setCount(64);
                        } else {
                            pill.grow(itemStack.getCount());
                        }
                        pills.set(i, pill);
                        return true;
                    }
                }
                this.pills.add(itemStack);
                return true;
            }
            return false;
        }

        public Optional<ItemStack> get() {
            if (this.index == -1 && !this.pills.isEmpty()) {
                return Optional.of(this.pills.removeFirst());
            }
            if (this.index < 0 || this.index > this.pills.size() - 1) {
                return Optional.empty();
            }
            return Optional.of(this.pills.remove(this.index));
        }

        public void useAll(Player player) {
            ObjectArrayList<ItemStack> pillList = new ObjectArrayList<>(8);
            Iterator<ItemStack> iterator = this.pills.iterator();
            while (iterator.hasNext()) {
                ItemStack pill = iterator.next();
                if (pill.is(ModFoodItems.PILL)) {
                    PillItem.use(pill, player);
                    pill.shrink(1);
                    if (pill.getCount() <= 0) {
                        iterator.remove();
                    } else {
                        pillList.add(pill);
                    }
                }
            }
            this.pills = pillList;
        }

        public PillBocContents immutable() {
            return new PillBocContents(this.index, ImmutableList.copyOf(this.pills));
        }
    }
}
