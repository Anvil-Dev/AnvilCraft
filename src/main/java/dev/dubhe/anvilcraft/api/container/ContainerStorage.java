package dev.dubhe.anvilcraft.api.container;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.item.ItemEntries;
import dev.dubhe.anvilcraft.api.container.level.ContainerLevel;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        ItemEntries.CODEC
            .optionalFieldOf("entries")
            .forGetter(ContainerStorage::getOpEntries),
        ICategory.CODEC.listOf()
            .fieldOf("categories")
            .forGetter(ContainerStorage::getCategories)
    ).apply(ins, ContainerStorage::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        ContainerStorage::getId,
        ContainerLevel.STREAM_CODEC,
        ContainerStorage::getLevel,
        ItemEntries.STREAM_CODEC,
        ContainerStorage::getEntries,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ContainerStorage::getCategories,
        ContainerStorage::new
    );
    private final UUID id;
    private ContainerLevel level = ContainerLevel.MIN;
    private final ItemEntries entries;
    private final List<ICategory> categories = new ArrayList<>();

    public ContainerStorage(UUID id) {
        this.id = id;
        this.entries = new ItemEntries();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private ContainerStorage(UUID id, ContainerLevel level, Optional<ItemEntries> entries, List<ICategory> categories) {
        this.id = id;
        this.level = level;
        this.entries = entries
            .map(entries1 -> Util.run(entries1, entries2 -> entries2.syncData(level)))
            .orElse(new ItemEntries(level));
        this.categories.addAll(categories);
    }

    private ContainerStorage(UUID id, ContainerLevel level, ItemEntries entries, List<ICategory> categories) {
        this.id = id;
        this.level = level;
        this.entries = entries;
        this.categories.addAll(categories);
        this.entries.syncData(level);
    }

    private Optional<ItemEntries> getOpEntries() {
        return this.entries.isEmpty() ? Optional.empty() : Optional.of(this.entries);
    }

    public boolean addItem(ItemStack stack) {
        return this.entries.add(new UnlimitedItemStack(stack));
    }

    public UnlimitedItemStack getItem(int index) {
        return ListUtil.safelyGet(this.entries, index).orElse(UnlimitedItemStack.EMPTY);
    }

    public int getMaxStackSize() {
        return Item.DEFAULT_MAX_STACK_SIZE * this.level.getStackPower();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * this.level.getStackPower();
    }

    public void setChanged() {
        this.entries.syncData(this.level);
        this.entries.setChanged();
    }

    public boolean upgrade() {
        if (this.level.ordinal() + 1 == ContainerLevel.values().length) return false;
        this.level = ContainerLevel.values()[this.level.ordinal() + 1];
        this.entries.syncData(this.level);
        return true;
    }

    public void sync(ContainerStorage storage) {
        this.level = storage.level;
        this.entries.getEntries().clear();
        this.entries.getEntries().addAll(storage.entries.getEntries());
        this.entries.setChanged();
        this.categories.clear();
        this.categories.addAll(storage.categories);
    }

    public void applyCategory(ContainerStorage source) {
        this.categories.clear();
        this.categories.addAll(source.categories);
    }
}
