package dev.dubhe.anvilcraft.api.recipe.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface INumberProvider {
    Codec<INumberProvider> TYPED_CODEC = Codec.lazyInitialized(() -> ModRegistries.NUMBER_PROVIDER_TYPE
        .byNameCodec().dispatch(INumberProvider::type, Type::codec));
    Codec<INumberProvider> CODEC = Codec.lazyInitialized(() -> Codec.either(INumberProvider.TYPED_CODEC, ConstantValue.INLINE_CODEC).xmap(
        Either::unwrap,
        provider -> provider instanceof ConstantValue constant ? Either.right(constant) : Either.left(provider)
    ));
    StreamCodec<RegistryFriendlyByteBuf, INumberProvider> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.NUMBER_PROVIDER_TYPE)
        .dispatch(INumberProvider::type, Type::streamCodec);

    float getFloat(ResultContext ctx);

    default int getInt(ResultContext ctx) {
        return Math.round(this.getFloat(ctx));
    }

    Type<? extends INumberProvider> type();

    interface Type<T extends INumberProvider> extends ISerializer<T> {
    }
}
