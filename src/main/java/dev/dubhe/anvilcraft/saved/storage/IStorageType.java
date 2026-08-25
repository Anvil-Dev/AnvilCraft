package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.util.RegistryUtil;
import dev.dubhe.anvilcraft.util.ResourceLocationUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public interface IStorageType<T extends BaseStorage<?>> {
    Codec<IStorageType<?>> CODEC = Codec.lazyInitialized(() -> RegistryUtil.referenceHolderWithLifecycle(
        ResourceLocationUtil.ANC_CODEC,
        ModRegistries.STORAGE_TYPE
    ).flatComapMap(
        Holder.Reference::value,
        value -> RegistryUtil.safeCastToReference(ModRegistryKeys.STORAGE_TYPE, ModRegistries.STORAGE_TYPE.wrapAsHolder(value))
    ));
    Codec<Holder<IStorageType<?>>> HOLDER_CODEC = Codec.lazyInitialized(() -> RegistryUtil.referenceHolderWithLifecycle(
        ResourceLocationUtil.ANC_CODEC,
        ModRegistries.STORAGE_TYPE
    ).flatComapMap(
        Function.identity(),
        value -> RegistryUtil.safeCastToReference(ModRegistryKeys.STORAGE_TYPE, value)
    ));
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
