package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;

import java.util.UUID;
import java.util.function.Function;

public record SignedPlayers(HashBiMap<Component, UUID> playerInfos) {
    public static final SignedPlayers EMPTY = new SignedPlayers(HashBiMap.create());
    private static final Codec<Component> NAME_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
            return Codec.STRING.decode(ops, input).flatMap(pair -> {
                try {
                    return ComponentSerialization.CODEC.parse(SignedPlayers.asJsonOps(ops), JsonParser.parseString(pair.getFirst()))
                        .map(component -> Pair.of(component, pair.getSecond()));
                } catch (JsonParseException exception) {
                    return DataResult.error(exception::toString);
                }
            });
        }

        @Override
        public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
            return ComponentSerialization.CODEC.encodeStart(SignedPlayers.asJsonOps(ops), input).flatMap(json -> {
                try {
                    return Codec.STRING.encodeStart(ops, GsonHelper.toStableString(json));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(exception::toString);
                }
            });
        }
    };
    public static final Codec<SignedPlayers> CODEC = Codec.unboundedMap(SignedPlayers.NAME_CODEC, UUIDUtil.CODEC)
        .xmap(HashBiMap::create, Function.identity())
        .xmap(SignedPlayers::new, SignedPlayers::playerInfos);
    public static final StreamCodec<RegistryFriendlyByteBuf, SignedPlayers> STREAM_CODEC = ByteBufCodecs.map(
        HashBiMap::create, ComponentSerialization.STREAM_CODEC, UUIDUtil.STREAM_CODEC
    ).map(SignedPlayers::new, SignedPlayers::playerInfos);

    private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> ops) {
        return ops instanceof RegistryOps<T> registryOps ? registryOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE;
    }
}
