package dev.dubhe.anvilcraft.api.container;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.item.ItemEntries;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@Getter
public class ContainerStorage {
    public static final MapCodec<ContainerStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("id")
            .forGetter(ContainerStorage::getId),
        ItemEntries.CODEC
            .forGetter(ContainerStorage::getEntries),
        ICategory.CODEC
            .listOf()
            .fieldOf("categories")
            .forGetter(ContainerStorage::getCategories),
        Upgrades.CODEC.codec()
            .fieldOf("upgrades")
            .forGetter(ContainerStorage::getUpgrades)
    ).apply(ins, ContainerStorage::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        ContainerStorage::getId,
        ItemEntries.STREAM_CODEC,
        ContainerStorage::getEntries,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ContainerStorage::getCategories,
        Upgrades.STREAM_CODEC,
        ContainerStorage::getUpgrades,
        ContainerStorage::new
    );
    private Component name = ModBlocks.SHULKER_CONTAINER.get().getName();
    private final UUID id;
    private final ItemEntries entries;
    private final List<ICategory> categories = new ArrayList<>();
    private final Upgrades upgrades = new Upgrades();

    public ContainerStorage(UUID id) {
        this.id = id;
        this.entries = new ItemEntries(this.upgrades);
    }

    private ContainerStorage(UUID id, ItemEntries entries, List<ICategory> categories, Upgrades upgrades) {
        this(id);
        this.entries.sync(entries, upgrades);
        this.categories.addAll(categories);
        this.upgrades.sync(upgrades);
    }

    public int addItem(ItemStack stack) {
        this.markDirty();
        return this.entries.addItem(stack);
    }

    public UnlimitedItemStack getItem(int index) {
        return this.entries.getItem(index);
    }

    public ItemStack splitUnchecked(int index, int amount) {
        this.markDirty();
        return this.entries.split(index, amount).toStack();
    }

    public ItemStack split(int index, int amount) {
        UnlimitedItemStack stack = this.getItem(index);
        amount = Math.min(amount, stack.getStack().getMaxStackSize());
        return this.splitUnchecked(index, amount);
    }

    public int getMaxStackSize() {
        return Item.DEFAULT_MAX_STACK_SIZE * this.upgrades.getStackPower();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * this.upgrades.getStackPower();
    }

    public boolean isFull(UnlimitedItemStack stack) {
        return this.getMaxStackSize(stack.getStack()) <= stack.getCount();
    }

    public boolean isMaxEntries() {
        return this.entries.entrySize() >= this.upgrades.getEntryLimit();
    }

    public Int2BooleanMap getOrder(
        Predicate<UnlimitedItemStack> filter,
        Comparator<UnlimitedItemStack> sorter,
        boolean shouldFold
    ) {
        return this.entries.getOrder(filter, sorter, shouldFold);
    }

    public void sync(ContainerStorage storage) {
        this.entries.clear();
        this.entries.sync(storage.entries, storage.upgrades);
        this.categories.clear();
        this.categories.addAll(storage.categories);
        this.upgrades.sync(storage.upgrades);
        this.markDirty();
    }

    public void applyCategory(ContainerStorage source) {
        this.categories.clear();
        this.categories.addAll(source.categories);
        this.markDirty();
    }

    private void markDirty() {
        ContainerStorages.get().setDirty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ContainerStorage storage)) return false;
        return Objects.equals(this.getId(), storage.getId())
               && Objects.equals(this.getEntries(), storage.getEntries())
               && Objects.equals(this.getCategories(), storage.getCategories())
               && Objects.equals(this.getUpgrades(), storage.getUpgrades());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getEntries(), this.getCategories(), this.getUpgrades());
    }
}
