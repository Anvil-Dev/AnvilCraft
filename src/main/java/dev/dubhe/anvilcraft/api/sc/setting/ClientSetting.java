package dev.dubhe.anvilcraft.api.sc.setting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.category.CategoryMode;
import dev.dubhe.anvilcraft.api.sc.category.provider.CategoryProvider;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ClientSetting(
    String searching,
    String searchMode,
    String sortMode,
    String sortOrderMode,
    String nbtDisplayMode,
    Map<UUID, Map<CategoryProvider, CategoryMode>> categorySetting
) {
    public static final MapCodec<ClientSetting> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .fieldOf("searching")
            .forGetter(ClientSetting::searching),
        Codec.STRING
            .fieldOf("searchMode")
            .forGetter(ClientSetting::searchMode),
        Codec.STRING
            .fieldOf("sortMode")
            .forGetter(ClientSetting::sortMode),
        Codec.STRING
            .fieldOf("sortOrderMode")
            .forGetter(ClientSetting::sortOrderMode),
        Codec.STRING
            .fieldOf("nbtDisplayMode")
            .forGetter(ClientSetting::nbtDisplayMode),
        Codec.unboundedMap(UUIDUtil.CODEC, Codec.unboundedMap(CategoryProvider.CODEC, CategoryMode.CODEC))
            .fieldOf("categorySetting")
            .forGetter(ClientSetting::categorySetting)
    ).apply(ins, ClientSetting::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientSetting> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ClientSetting::searching,
        ByteBufCodecs.STRING_UTF8,
        ClientSetting::searchMode,
        ByteBufCodecs.STRING_UTF8,
        ClientSetting::sortMode,
        ByteBufCodecs.STRING_UTF8,
        ClientSetting::sortOrderMode,
        ByteBufCodecs.STRING_UTF8,
        ClientSetting::nbtDisplayMode,
        ByteBufCodecs.map(
            HashMap::new,
            UUIDUtil.STREAM_CODEC,
            ByteBufCodecs.map(HashMap::new, CategoryProvider.STREAM_CODEC, CategoryMode.STREAM_CODEC)
        ),
        ClientSetting::categorySetting,
        ClientSetting::new
    );
}
