package dev.dubhe.anvilcraft.api.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModNumberExpressionTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 具名传入值表达式，引用 {@link NumberArguments#value(String)}，在 flat 表达式中写作 {@code $(name)}。
 */
public record NamedExpression(String name) implements INumberExpression {
    public static final MapCodec<NamedExpression> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .fieldOf("name")
            .forGetter(NamedExpression::name)
    ).apply(ins, NamedExpression::new));
    public static final StreamCodec<ByteBuf, NamedExpression> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        NamedExpression::name,
        NamedExpression::new
    );

    public static NamedExpression of(String name) {
        return new NamedExpression(name);
    }

    @Override
    public double evaluate(NumberArguments inputs) {
        return inputs.value(this.name);
    }

    @Override
    public Type type() {
        return ModNumberExpressionTypes.NAMED.get();
    }

    public static class Type implements INumberExpression.Type<NamedExpression> {
        @Override
        public MapCodec<NamedExpression> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NamedExpression> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
