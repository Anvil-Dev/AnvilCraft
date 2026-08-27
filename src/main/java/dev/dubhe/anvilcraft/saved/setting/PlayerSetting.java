package dev.dubhe.anvilcraft.saved.setting;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.init.storage.ModCategories;
import dev.dubhe.anvilcraft.saved.setting.mode.BalanceMode;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record PlayerSetting(List<CategoryEntry> listed, List<ICategory> custom, StorageSetting storage, BalanceMode balanceMode) {
    public static final MapCodec<PlayerSetting> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CategoryEntry.CODEC
            .codec()
            .listOf()
            .fieldOf("listed")
            .forGetter(PlayerSetting::listed),
        ICategory.CODEC
            .listOf()
            .fieldOf("custom")
            .forGetter(PlayerSetting::custom),
        StorageSetting.CODEC
            .forGetter(PlayerSetting::storage),
        BalanceMode.CODEC
            .fieldOf("balance_mode")
            .orElse(BalanceMode.RESTOCK)
            .forGetter(PlayerSetting::balanceMode)
    ).apply(instance, PlayerSetting::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSetting> STREAM_CODEC = StreamCodec.composite(
        CategoryEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PlayerSetting::listed,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PlayerSetting::custom,
        StorageSetting.STREAM_CODEC,
        PlayerSetting::storage,
        BalanceMode.STREAM_CODEC,
        PlayerSetting::balanceMode,
        PlayerSetting::new
    );

    public PlayerSetting(List<CategoryEntry> listed, List<ICategory> custom, StorageSetting storage) {
        this(listed, custom, storage, BalanceMode.RESTOCK);
    }

    public PlayerSetting(List<CategoryEntry> listed, List<ICategory> custom, StorageSetting storage, BalanceMode balanceMode) {
        this.listed = new ArrayList<>(listed);
        this.custom = new ArrayList<>(custom);
        this.storage = storage;
        this.balanceMode = balanceMode;
    }

    public PlayerSetting(HolderLookup.Provider registries) {
        this(PlayerSetting.initialize(registries), new ArrayList<>(), new StorageSetting());
    }

    private static List<CategoryEntry> initialize(HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<ICategory> lookup = registries.lookupOrThrow(ModRegistryKeys.CATEGORY);
        return Lists.newArrayList(
            new CategoryEntry(lookup.getOrThrow(ModCategories.MINECRAFT).value()),
            new CategoryEntry(lookup.getOrThrow(ModCategories.BLOCK).value()),
            new CategoryEntry(lookup.getOrThrow(ModCategories.UNSTACKABLE).value())
        );
    }

    public void list(ICategory category) {
        this.listed.add(new CategoryEntry(category));
    }

    public CategoryEntry unlist(int index) {
        return this.listed.remove(index);
    }

    public void pinToTop(int index) {
        this.listed.addFirst(this.listed.remove(index));
    }

    public void addCustom(ICategory category) {
        this.custom.add(category);
    }

    public void addCustom(ItemStack filter) {
        this.custom.add(FilterCategory.from(filter));
    }
}
