package dev.dubhe.anvilcraft.api.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModNumberExpressionTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * 传入值表达式，按下标引用 {@link NumberArguments} 中的传入值。
 */
public record InputExpression(int index) implements INumberExpression {
    public static final MapCodec<InputExpression> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ExtraCodecs.NON_NEGATIVE_INT
            .fieldOf("index")
            .forGetter(InputExpression::index)
    ).apply(ins, InputExpression::new));
    public static final StreamCodec<ByteBuf, InputExpression> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        InputExpression::index,
        InputExpression::new
    );

    public static InputExpression of(int index) {
        return new InputExpression(index);
    }

    @Override
    public double evaluate(NumberArguments inputs) {
        return inputs.value(this.index);
    }

    @Override
    public Type type() {
        return ModNumberExpressionTypes.INPUT.get();
    }

    public static class Type implements INumberExpression.Type<InputExpression> {
        @Override
        public MapCodec<InputExpression> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InputExpression> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
