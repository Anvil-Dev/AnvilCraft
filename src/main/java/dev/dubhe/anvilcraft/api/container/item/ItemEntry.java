package dev.dubhe.anvilcraft.api.container.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.util.CodecUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

public final class ItemEntry {
    public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        CodecUtil.ITEM_CODEC
            .fieldOf("item")
            .forGetter(ItemEntry::item),
        EntryData.CODEC.codec().listOf()
            .fieldOf("data")
            .forGetter(ItemEntry::data)
    ).apply(ins, ItemEntry::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntry> STREAM_CODEC = StreamCodec.composite(
        CodecUtil.ITEM_STREAM_CODEC,
        ItemEntry::item,
        EntryData.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ItemEntry::data,
        ItemEntry::new
    );
    private final Item item;
    private final List<EntryData> data;
    private boolean cached = false;
    private List<ItemStack> cache;

    public ItemEntry(Item item, List<EntryData> data) {
        this.item = item;
        this.data = data;
    }

    public static ItemEntry of(ItemStack stack) {
        List<EntryData> data = new ArrayList<>();
        data.add(new EntryData(stack.getComponentsPatch(), stack.getCount()));
        return new ItemEntry(stack.getItem(), data);
    }

    public boolean is(Item item) {
        return this.item.equals(item);
    }

    public boolean is(ItemStack stack) {
        return stack.is(this.item);
    }

    public InteractionResult merge(ItemStack stack, int stackPower) {
        if (!this.item.equals(stack.getItem())) return InteractionResult.FAIL;
        this.cached = false;
        DataComponentPatch patch = stack.getComponentsPatch();
        for (EntryData entry1 : this.data) {
            if (!entry1.getPatch().equals(patch)) continue;
            int count = entry1.getCount() + stack.getCount();
            int maxSize = stack.getMaxStackSize() * stackPower;
            InteractionResult result = InteractionResult.CONSUME;
            if (count > maxSize) {
                stack.setCount(stack.getCount() - (maxSize - count));
                count = maxSize;
                result = InteractionResult.CONSUME_PARTIAL;
            }
            entry1.setCount(count);
            return result;
        }
        return InteractionResult.FAIL;
    }

    public TriState modifyCount(ItemStack data, IntUnaryOperator operator) {
        DataComponentPatch patch = data.getComponentsPatch();
        for (EntryData entry : this.data) {
            if (!entry.getPatch().equals(patch)) continue;
            int count = operator.applyAsInt(entry.count);
            if (count < 0) return TriState.FALSE;
            if (count == 0) {
                if (this.data.size() == 1) {
                    return TriState.DEFAULT;
                } else {
                    this.data.removeIf(entry1 -> entry1.patch.equals(entry.patch));
                    return this.data.isEmpty() ? TriState.DEFAULT : TriState.TRUE;
                }
            }
            entry.setCount(count);
            return TriState.TRUE;
        }
        return TriState.FALSE;
    }

    public List<ItemStack> toStacks() {
        if (this.cached) return this.cache;

        this.cached = true;
        final int maxSize = this.item.getDefaultMaxStackSize();
        List<ItemStack> stacks = new ArrayList<>();
        for (EntryData data : this.data) {
            int count = data.count;
            DataComponentPatch patch = data.patch;
            while (count > 0) {
                int thisCount;
                if (count > maxSize) {
                    thisCount = maxSize;
                    count -= maxSize;
                } else {
                    thisCount = count;
                    count = 0;
                }
                //noinspection deprecation
                stacks.add(new ItemStack(this.item.builtInRegistryHolder(), thisCount, patch));
            }
        }
        this.cache = stacks;
        return stacks;
    }

    public Item item() {
        return this.item;
    }

    public List<EntryData> data() {
        return this.data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ItemEntry) obj;
        return Objects.equals(this.item, that.item)
               && Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.item, this.data);
    }

    @Override
    public String toString() {
        return "ItemEntry["
               + "item=" + this.item + ", "
               + "data=" + this.data + ']';
    }

    @Getter
    @Setter
    public static final class EntryData {
        public static final MapCodec<EntryData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            DataComponentPatch.CODEC.fieldOf("patch").forGetter(EntryData::getPatch),
            Codec.INT.fieldOf("count").forGetter(EntryData::getCount)
        ).apply(ins, EntryData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryData> STREAM_CODEC = StreamCodec.composite(
            DataComponentPatch.STREAM_CODEC,
            EntryData::getPatch,
            ByteBufCodecs.VAR_INT,
            EntryData::getCount,
            EntryData::new
        );
        private final DataComponentPatch patch;
        private int count;

        public EntryData(DataComponentPatch patch, int count) {
            this.patch = patch;
            this.count = count;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (EntryData) obj;
            return Objects.equals(this.patch, that.patch)
                   && this.count == that.count;
        }

        @Override
        public int hashCode() {
            return Objects.hash(patch, count);
        }

        @Override
        public String toString() {
            return "EntryData["
                   + "patch=" + patch + ", "
                   + "count=" + count + ']';
        }

    }
}
