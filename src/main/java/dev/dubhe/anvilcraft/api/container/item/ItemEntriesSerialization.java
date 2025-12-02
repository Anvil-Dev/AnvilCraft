package dev.dubhe.anvilcraft.api.container.item;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter(AccessLevel.PRIVATE)
class ItemEntriesSerialization {
    public static final MapCodec<ItemEntriesSerialization> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ItemEntry.CODEC.codec()
            .listOf()
            .fieldOf("entries")
            .forGetter(ItemEntriesSerialization::getEntries)
    ).apply(ins, ItemEntriesSerialization::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntriesSerialization> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(ArrayList::new, ItemEntry.STREAM_CODEC),
        ItemEntriesSerialization::getEntries,
        ItemEntriesSerialization::new
    );
    private final List<ItemEntry> entries;

    private ItemEntriesSerialization(List<ItemEntry> entries) {
        this.entries = entries;
    }

    ItemEntriesSerialization(ItemEntries entries) {
        ImmutableList.Builder<ItemEntry> entriesBuilder = ImmutableList.builder();
        for (Map.Entry<Holder<Item>, IntList> entry : entries.entries.entrySet()) {
            Holder<Item> item = entry.getKey();

            IntList stackIndexes = entry.getValue();
            ImmutableList.Builder<EntryData> data = ImmutableList.builder();
            for (int stackIndex : stackIndexes) {
                var stackOp = ListUtil.safelyGet(entries.stacks, stackIndex);
                if (stackOp.isEmpty()) continue;
                var stack = stackOp.get();
                data.add(new EntryData(stack.getCount(), stack.getComponentsPatch()));
            }

            entriesBuilder.add(new ItemEntry(item, data.build()));
        }
        this.entries = entriesBuilder.build();
    }

    ItemEntries toEntries() {
        List<UnlimitedItemStack> stacks = new ArrayList<>();
        Map<Holder<Item>, IntList> entries = new HashMap<>();

        for (ItemEntry entry : this.entries) {
            IntList stackIndexes = new IntArrayList();
            for (EntryData data : entry.data) {
                int index = stacks.size();
                stacks.add(index, new UnlimitedItemStack(entry.item, data.count, data.patch));
                stackIndexes.add(index);
            }
            entries.put(entry.item, stackIndexes);
        }

        return new ItemEntries(stacks, entries);
    }

    record ItemEntry(Holder<Item> item, List<EntryData> data) {
        public static final MapCodec<ItemEntry> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            BuiltInRegistries.ITEM.holderByNameCodec()
                .fieldOf("item")
                .forGetter(ItemEntry::item),
            EntryData.CODEC.codec()
                .listOf()
                .fieldOf("data")
                .forGetter(ItemEntry::data)
        ).apply(ins, ItemEntry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ITEM),
            ItemEntry::item,
            ByteBufCodecs.collection(ArrayList::new, EntryData.STREAM_CODEC),
            ItemEntry::data,
            ItemEntry::new
        );
    }

    record EntryData(int count, DataComponentPatch patch) {
        public static final MapCodec<EntryData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.INT
                .fieldOf("count")
                .forGetter(EntryData::count),
            DataComponentPatch.CODEC
                .fieldOf("patch")
                .forGetter(EntryData::patch)
        ).apply(ins, EntryData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EntryData::count,
            DataComponentPatch.STREAM_CODEC,
            EntryData::patch,
            EntryData::new
        );
    }
}
