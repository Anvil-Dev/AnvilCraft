package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlayerSetting(List<CategoryEntry> listed, List<ICategory> custom, Map<UUID, StorageSetting> storageSettings) {
    public static final MapCodec<PlayerSetting> CODEC = CodecUtil.mapCodec(
        CategoryEntry.CODEC.codec()
            .listOf()
            .fieldOf("listed")
            .forGetter(PlayerSetting::listed),
        ICategory.CODEC
            .listOf()
            .fieldOf("custom")
            .forGetter(PlayerSetting::custom),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, StorageSetting.CODEC.codec())
            .fieldOf("storageSettings")
            .forGetter(PlayerSetting::storageSettings),
        PlayerSetting::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSetting> STREAM_CODEC = StreamCodec.composite(
        CategoryEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PlayerSetting::listed,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PlayerSetting::custom,
        ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, StorageSetting.STREAM_CODEC),
        PlayerSetting::storageSettings,
        PlayerSetting::new
    );

    public PlayerSetting() {
        this(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
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
