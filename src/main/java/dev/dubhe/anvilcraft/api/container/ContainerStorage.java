package dev.dubhe.anvilcraft.api.container;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.item.ItemEntries;
import dev.dubhe.anvilcraft.api.container.upgrade.Upgrades;
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
        ItemEntries.CODEC
            .optionalFieldOf("entries")
            .forGetter(ContainerStorage::getOpEntries),
        ICategory.CODEC.listOf()
            .fieldOf("categories")
            .forGetter(ContainerStorage::getCategories),
        Upgrades.CODEC
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
    private final UUID id;
    private final ItemEntries entries;
    private final List<ICategory> categories = new ArrayList<>();
    private final Upgrades upgrades = new Upgrades();

    public ContainerStorage(UUID id) {
        this.id = id;
        this.entries = new ItemEntries(this.upgrades);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private ContainerStorage(UUID id, Optional<ItemEntries> entries, List<ICategory> categories, Upgrades upgrades) {
        this.id = id;
        this.entries = entries
            .map(entries1 -> Util.run(entries1, entries2 -> entries2.syncData(upgrades)))
            .orElse(new ItemEntries(upgrades));
        this.categories.addAll(categories);
        this.upgrades.sync(upgrades);
    }

    private ContainerStorage(UUID id, ItemEntries entries, List<ICategory> categories, Upgrades upgrades) {
        this.id = id;
        this.entries = entries;
        this.categories.addAll(categories);
        this.upgrades.sync(upgrades);

        this.entries.syncData(upgrades);
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
        return Item.DEFAULT_MAX_STACK_SIZE * this.upgrades.getStackPower();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * this.upgrades.getStackPower();
    }

    public boolean isFull(UnlimitedItemStack stack) {
        return stack.getStack().getMaxStackSize() * this.upgrades.getStackPower() >= stack.getCount();
    }

    public void setChanged() {
        this.entries.syncData(this.upgrades);
        this.entries.setChanged();
    }

    public void sync(ContainerStorage storage) {
        this.entries.getEntries().clear();
        this.entries.getEntries().addAll(storage.entries.getEntries());
        this.entries.setChanged();
        this.categories.clear();
        this.categories.addAll(storage.categories);
        this.upgrades.sync(storage.upgrades);
    }

    public void applyCategory(ContainerStorage source) {
        this.categories.clear();
        this.categories.addAll(source.categories);
    }
}
