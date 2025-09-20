package dev.dubhe.anvilcraft.api.container;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.item.ItemEntry;
import dev.dubhe.anvilcraft.api.container.level.ContainerLevel;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class ContainerStorage {
    public static final MapCodec<ContainerStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("id")
            .forGetter(ContainerStorage::getId),
        ContainerLevel.CODEC
            .fieldOf("level")
            .forGetter(ContainerStorage::getLevel),
        ItemEntry.CODEC.listOf()
            .fieldOf("entries")
            .forGetter(ContainerStorage::getEntries),
        ICategory.CODEC.listOf()
            .fieldOf("categories")
            .forGetter(ContainerStorage::getCategories)
    ).apply(ins, ContainerStorage::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        ContainerStorage::getId,
        ContainerLevel.STREAM_CODEC,
        ContainerStorage::getLevel,
        ItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ContainerStorage::getEntries,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ContainerStorage::getCategories,
        ContainerStorage::new
    );
    private final UUID id;
    private ContainerLevel level = ContainerLevel.MIN;
    private final List<ItemEntry> entries = new ArrayList<>();
    private final List<ICategory> categories = new ArrayList<>();

    public ContainerStorage(UUID id) {
        this.id = id;
    }

    private ContainerStorage(UUID id, ContainerLevel level, List<ItemEntry> entries, List<ICategory> categories) {
        this.id = id;
        this.level = level;
        this.entries.addAll(entries);
        this.categories.addAll(categories);
    }

    public InteractionResult addItem(ItemStack stack) {
        for (ItemEntry entry : this.entries) {
            if (!entry.is(stack)) continue;
            return entry.merge(stack, this.level.getStackPower());
        }
        if (this.entries.size() == this.level.getEntryLimit()) return InteractionResult.FAIL;
        this.entries.add(ItemEntry.of(stack));
        return InteractionResult.CONSUME;
    }

    public boolean upgrade() {
        if (this.level.ordinal() + 1 == ContainerLevel.values().length) return false;
        this.level = ContainerLevel.values()[this.level.ordinal() + 1];
        return true;
    }
}
