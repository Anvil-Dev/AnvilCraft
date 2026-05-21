package dev.dubhe.anvilcraft.api.uuid;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface IUuidProvider extends Supplier<UUID> {
    Codec<IUuidProvider> CODEC = Codec.lazyInitialized(() -> ModRegistries.UUID_PROVIDER_TYPE_REGISTRY
        .byNameCodec().dispatch(IUuidProvider::getType, Type::codec));
    StreamCodec<RegistryFriendlyByteBuf, IUuidProvider> STREAM_CODEC = StreamCodec.recursive(
        streamCodec -> ByteBufCodecs.registry(ModRegistries.UUID_PROVIDER_TYPE_KEY)
            .dispatch(IUuidProvider::getType, Type::streamCodec));

    @Override
    UUID get();

    boolean isEmpty();

    default boolean canGet() {
        return true;
    }

    default Optional<UUID> optionalGet() {
        return this.canGet() ? Optional.of(this.get()) : Optional.empty();
    }

    /**
     * 获取该UUID提供器的类型。
     *
     * @return 该UUID提供器的类型
     */
    IUuidProvider.Type<? extends IUuidProvider> getType();

    interface Type<T extends IUuidProvider> extends ISerializer<T> {
    }
}
