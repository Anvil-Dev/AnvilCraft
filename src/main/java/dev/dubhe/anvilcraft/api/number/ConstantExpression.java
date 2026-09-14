package dev.dubhe.anvilcraft.api.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModNumberExpressionTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ConstantExpression(double value) implements INumberExpression {
    public static final MapCodec<ConstantExpression> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.DOUBLE
            .fieldOf("value")
            .forGetter(ConstantExpression::value)
    ).apply(ins, ConstantExpression::new));
    public static final Codec<ConstantExpression> INLINE_CODEC = Codec.either(Codec.INT, Codec.DOUBLE).xmap(
        either -> new ConstantExpression(either.map(Integer::doubleValue, Double::doubleValue)),
        constant -> constant.value() - Math.floor(constant.value()) < 1.0E-5
                    ? Either.left((int) constant.value())
                    : Either.right(constant.value())
    );
    public static final StreamCodec<ByteBuf, ConstantExpression> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        ConstantExpression::value,
        ConstantExpression::new
    );

    public static ConstantExpression of(double value) {
        return new ConstantExpression(value);
    }

    @Override
    public double evaluate(double... inputs) {
        return this.value;
    }

    @Override
    public Type type() {
        return ModNumberExpressionTypes.CONSTANT.get();
    }

    public static class Type implements INumberExpression.Type<ConstantExpression> {
        @Override
        public MapCodec<ConstantExpression> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConstantExpression> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
