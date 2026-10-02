package dev.dubhe.anvilcraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;

public final class ComponentCodecs {
    private ComponentCodecs() {
    }

    public static final Codec<Component> FLAT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
            return Codec.STRING.decode(ops, input).flatMap(pair -> {
                try {
                    return ComponentSerialization.CODEC.parse(ComponentCodecs.asJsonOps(ops), JsonParser.parseString(pair.getFirst()))
                        .map(component -> Pair.of(component, pair.getSecond()));
                } catch (JsonParseException exception) {
                    return DataResult.error(exception::toString);
                }
            });
        }

        @Override
        public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
            return ComponentSerialization.CODEC.encodeStart(ComponentCodecs.asJsonOps(ops), input).flatMap(json -> {
                try {
                    return Codec.STRING.encodeStart(ops, GsonHelper.toStableString(json));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(exception::toString);
                }
            });
        }
    };

    private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> ops) {
        return ops instanceof RegistryOps<T> registryOps ? registryOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE;
    }
}
