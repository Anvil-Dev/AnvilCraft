package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface IStorageType<T extends BaseStorage<?>> {
    Codec<IStorageType<?>> CODEC = ModRegistries.STORAGE_TYPE.byNameCodec();
    Codec<Holder<IStorageType<?>>> HOLDER_CODEC = ModRegistries.STORAGE_TYPE.holderByNameCodec();
    StreamCodec<RegistryFriendlyByteBuf, IStorageType<?>> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.STORAGE_TYPE);
    StreamCodec<RegistryFriendlyByteBuf, Holder<IStorageType<?>>> HOLDER_STREAM_CODEC = ByteBufCodecs.holderRegistry(
        ModRegistryKeys.STORAGE_TYPE
    );
    Map<Class<? extends BaseStorage<?>>, Holder<IStorageType<?>>> CLASS_MAP = new HashMap<>();

    static <T extends BaseStorage<?>> Holder<IStorageType<T>> find(Class<T> clazz) {
        return Util.cast(IStorageType.CLASS_MAP.get(clazz));
    }

    T newInstance(UUID id);

    Class<T> clazz();
}
