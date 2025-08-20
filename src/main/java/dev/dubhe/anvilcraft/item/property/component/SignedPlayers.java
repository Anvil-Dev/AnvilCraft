package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;
import java.util.function.Function;

public record SignedPlayers(HashBiMap<Component, UUID> playerInfos) {
    public static final SignedPlayers EMPTY = new SignedPlayers(HashBiMap.create());
    public static final Codec<SignedPlayers> CODEC = Codec.unboundedMap(ComponentSerialization.FLAT_CODEC, UUIDUtil.CODEC)
        .xmap(HashBiMap::create, Function.identity())
        .xmap(SignedPlayers::new, SignedPlayers::playerInfos);
    public static final StreamCodec<RegistryFriendlyByteBuf, SignedPlayers> STREAM_CODEC = ByteBufCodecs.map(
        HashBiMap::create, ComponentSerialization.STREAM_CODEC, UUIDUtil.STREAM_CODEC
    ).map(SignedPlayers::new, SignedPlayers::playerInfos);
}
