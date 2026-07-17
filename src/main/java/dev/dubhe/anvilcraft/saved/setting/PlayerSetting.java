package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record PlayerSetting(List<CategoryEntry> listed, List<ICategory> custom, StorageSetting storage) {
    public static final MapCodec<PlayerSetting> CODEC = CodecUtil.mapCodec(
        CategoryEntry.CODEC.codec()
            .listOf()
            .fieldOf("listed")
            .forGetter(PlayerSetting::listed),
        ICategory.CODEC
            .listOf()
            .fieldOf("custom")
            .forGetter(PlayerSetting::custom),
        StorageSetting.CODEC
            .forGetter(PlayerSetting::storage),
        PlayerSetting::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSetting> STREAM_CODEC = StreamCodec.composite(
        CategoryEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PlayerSetting::listed,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PlayerSetting::custom,
        StorageSetting.STREAM_CODEC,
        PlayerSetting::storage,
        PlayerSetting::new
    );

    public PlayerSetting() {
        this(new ArrayList<>(), new ArrayList<>(), new StorageSetting());
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
