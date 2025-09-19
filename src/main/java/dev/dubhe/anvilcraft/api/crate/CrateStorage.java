package dev.dubhe.anvilcraft.api.crate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.api.crate.category.ICategory;
import dev.dubhe.anvilcraft.api.crate.level.CrateLevel;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class CrateStorage {
    public static final MapCodec<CrateStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(CrateStorage::getId),
        CrateLevel.CODEC.fieldOf("level").forGetter(CrateStorage::getLevel),
        ItemEntry.CODEC.listOf().fieldOf("entries").forGetter(CrateStorage::getEntries),
        ICategory.CODEC.listOf().fieldOf("categories").forGetter(CrateStorage::getCategories)
    ).apply(ins, CrateStorage::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        CrateStorage::getId,
        CrateLevel.STREAM_CODEC,
        CrateStorage::getLevel,
        ItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        CrateStorage::getEntries,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        CrateStorage::getCategories,
        CrateStorage::new
    );
    private final UUID id;
    private CrateLevel level = CrateLevel.MIN;
    private final List<ItemEntry> entries = new ArrayList<>(this.level.getEntryLimit());
    private final List<ICategory> categories = new ArrayList<>();

    public CrateStorage(UUID id) {
        this.id = id;
    }

    private CrateStorage(UUID id, CrateLevel level, List<ItemEntry> entries, List<ICategory> categories) {
        this.id = id;
        this.level = level;
        this.entries.addAll(entries);
        this.categories.addAll(categories);
    }

    public boolean addItem(ItemStack stack) {
        for (ItemEntry entry : this.entries) {
            if (!entry.is(stack)) continue;
            entry.merge(stack, this.level.getStackPower());
            return true;
        }
        if (this.entries.size() == this.level.getEntryLimit()) return false;
        return this.entries.add(ItemEntry.of(stack));
    }

    public boolean upgrade() {
        if (this.level.ordinal() + 1 == CrateLevel.values().length) return false;
        this.level = CrateLevel.values()[this.level.ordinal() + 1];
        return true;
    }

    public record ItemEntry(Item item, List<EntryData> data) {
        public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            CodecUtil.ITEM_CODEC.fieldOf("item").forGetter(ItemEntry::item),
            EntryData.CODEC.codec().listOf().fieldOf("data").forGetter(ItemEntry::data)
        ).apply(ins, ItemEntry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntry> STREAM_CODEC = StreamCodec.composite(
            CodecUtil.ITEM_STREAM_CODEC,
            ItemEntry::item,
            EntryData.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemEntry::data,
            ItemEntry::new
        );

        public static ItemEntry of(ItemStack stack) {
            List<EntryData> data = new ArrayList<>();
            data.add(new EntryData(stack.getComponentsPatch(), stack.getCount()));
            return new ItemEntry(stack.getItem(), data);
        }

        public boolean is(ItemStack stack) {
            return stack.is(this.item);
        }

        public void merge(ItemStack stack, int stackPower) {
            if (!this.item.equals(stack.getItem())) return;
            DataComponentPatch patch = stack.getComponentsPatch();
            int i = -1;
            int count = -1;
            for (EntryData entry1 : this.data) {
                i++;
                if (!entry1.patch().equals(patch)) continue;
                count = entry1.count();
                break;
            }
            if (count == -1) return;
            this.data.set(i, new EntryData(patch, Math.min(stack.getCount() + count, stack.getMaxStackSize() * stackPower)));
        }
    }

    public record EntryData(DataComponentPatch patch, int count) {
        public static final MapCodec<EntryData> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            DataComponentPatch.CODEC.fieldOf("patch").forGetter(EntryData::patch),
            Codec.INT.fieldOf("count").forGetter(EntryData::count)
        ).apply(ins, EntryData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryData> STREAM_CODEC = StreamCodec.composite(
            DataComponentPatch.STREAM_CODEC,
            EntryData::patch,
            ByteBufCodecs.VAR_INT,
            EntryData::count,
            EntryData::new
        );
    }
}
