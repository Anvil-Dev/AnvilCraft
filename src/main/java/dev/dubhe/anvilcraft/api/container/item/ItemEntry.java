package dev.dubhe.anvilcraft.api.container.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
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
    private List<UnlimitedItemStack> cache;

    public ItemEntry(Item item, List<EntryData> data) {
        this.item = item;
        this.data = data;
    }

    public static ItemEntry of(ItemStack stack) {
        List<EntryData> data = new ArrayList<>();
        data.add(new EntryData(stack.getComponentsPatch(), stack.getCount()));
        return new ItemEntry(stack.getItem(), data);
    }

    public static ItemEntry of(UnlimitedItemStack stack) {
        List<EntryData> data = new ArrayList<>();
        data.add(new EntryData(stack.getStack().getComponentsPatch(), stack.getCount()));
        return new ItemEntry(stack.getStack().getItem(), data);
    }

    public boolean is(Item item) {
        return this.item.equals(item);
    }

    public boolean is(ItemStack stack) {
        return stack.is(this.item);
    }

    public InteractionResult merge(ItemStack stack, int stackPower) {
        if (!this.is(stack)) return InteractionResult.FAIL;
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

    public InteractionResult merge(UnlimitedItemStack stack, int stackPower) {
        if (!this.is(stack.getStack())) return InteractionResult.FAIL;
        this.cached = false;
        DataComponentPatch patch = stack.getStack().getComponentsPatch();
        for (EntryData entry1 : this.data) {
            if (!entry1.getPatch().equals(patch)) continue;
            int count = entry1.getCount() + stack.getCount();
            int maxSize = stack.getStack().getMaxStackSize() * stackPower;
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

    /**
     * 修改指定条目的数量。
     *
     * @param data 包含数据组件的物品组，用于匹配条目
     * @param operator 以条目原数量为参数的修改器，用于修改匹配条目的数量
     * @return 在返回的 {@link ModifyResult} 中，<br>
     *         {@link ModifyResult#result result} 为 {@link TriState#TRUE TRUE} 说明修改成功；<br>
     *         {@link ModifyResult#result result} 为 {@link TriState#DEFAULT DEFAULT} 说明修改失败，保持不变；<br>
     *         {@link ModifyResult#result result} 为 {@link TriState#FALSE FALSE} 说明修改成功且完成后该条目为空，需要删除。<br><br>
     *         {@link ModifyResult#stackCountChanged stackCountChanged} 为 {@link TriState#TRUE TRUE} 说明条目对应的物品堆数量有增加；<br>
     *         {@link ModifyResult#stackCountChanged stackCountChanged} 为 {@link TriState#DEFAULT DEFAULT} 说明条目对应的物品堆数量保持不变；<br>
     *         {@link ModifyResult#stackCountChanged stackCountChanged} 为 {@link TriState#FALSE FALSE} 说明条目对应的物品堆数量有减少。
     */
    public ModifyResult modifyCount(ItemStack data, int stackPower, IntUnaryOperator operator) {
        this.cached = false;
        DataComponentPatch patch = data.getComponentsPatch();
        for (EntryData entry : this.data) {
            if (!entry.getPatch().equals(patch)) continue;
            int count = operator.applyAsInt(entry.count);
            if (count < 0 || count > this.item.getDefaultMaxStackSize() * stackPower) {
                return new ModifyResult(TriState.DEFAULT, entry.count, TriState.DEFAULT);
            }
            if (count == 0) {
                if (this.data.size() == 1) {
                    return new ModifyResult(TriState.FALSE, entry.count, TriState.DEFAULT);
                } else {
                    this.data.removeIf(entry1 -> entry1.patch.equals(entry.patch));
                    return new ModifyResult(this.data.isEmpty() ? TriState.FALSE : TriState.TRUE, entry.count, TriState.FALSE);
                }
            }
            int oldCount = entry.count;
            entry.setCount(count);
            return new ModifyResult(TriState.TRUE, oldCount, TriState.DEFAULT);
        }
        int count = operator.applyAsInt(0);
        if (count <= 0 || count > this.item.getDefaultMaxStackSize() * stackPower) {
            return new ModifyResult(TriState.DEFAULT, 0, TriState.DEFAULT);
        }
        this.data.add(new EntryData(data.getComponentsPatch(), count));
        return new ModifyResult(TriState.TRUE, 0, TriState.TRUE);
    }

    public record ModifyResult(TriState result, int oldCount, TriState stackCountChanged) {
    }

    public List<UnlimitedItemStack> toStacks() {
        if (this.cached) return this.cache;

        this.cached = true;
        List<UnlimitedItemStack> stacks = new ArrayList<>();
        for (EntryData data : this.data) {
            //noinspection deprecation
            stacks.add(new UnlimitedItemStack(this.item.builtInRegistryHolder(), data.count, data.patch));
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
