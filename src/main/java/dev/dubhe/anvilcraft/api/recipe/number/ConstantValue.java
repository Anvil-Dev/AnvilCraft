package dev.dubhe.anvilcraft.api.recipe.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.init.recipe.ModNumberProviderTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ConstantValue(float value) implements INumberProvider {
    public static final MapCodec<ConstantValue> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.FLOAT
            .fieldOf("value")
            .forGetter(ConstantValue::value)
    ).apply(ins, ConstantValue::new));
    public static final Codec<ConstantValue> INLINE_CODEC = Codec.either(Codec.INT, Codec.FLOAT).xmap(
        either -> either.map(ConstantValue::new, ConstantValue::new),
        constant -> constant.value() - Math.floor(constant.value()) < 1.0E-5
                    ? Either.left((int) constant.value())
                    : Either.right(constant.value())
    );
    public static final StreamCodec<ByteBuf, ConstantValue> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        ConstantValue::value,
        ConstantValue::new
    );

    public ConstantValue(int value) {
        this((float) value);
    }

    public static ConstantValue exactly(float value) {
        return new ConstantValue(value);
    }

    @Override
    public float getFloat(ResultContext ctx) {
        return this.value;
    }

    @Override
    public Type type() {
        return ModNumberProviderTypes.CONSTANT.get();
    }

    public static class Type implements INumberProvider.Type<ConstantValue> {
        @Override
        public MapCodec<ConstantValue> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConstantValue> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
