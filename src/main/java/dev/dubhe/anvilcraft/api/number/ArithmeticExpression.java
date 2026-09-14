package dev.dubhe.anvilcraft.api.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModNumberExpressionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 四则运算表达式。
 */
public record ArithmeticExpression(
    Operator operator,
    INumberExpression left,
    INumberExpression right
) implements INumberExpression {
    public static final MapCodec<ArithmeticExpression> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Operator.CODEC
            .fieldOf("operator")
            .forGetter(ArithmeticExpression::operator),
        INumberExpression.CODEC
            .fieldOf("left")
            .forGetter(ArithmeticExpression::left),
        INumberExpression.CODEC
            .fieldOf("right")
            .forGetter(ArithmeticExpression::right)
    ).apply(ins, ArithmeticExpression::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArithmeticExpression> STREAM_CODEC = StreamCodec.composite(
        Operator.STREAM_CODEC,
        ArithmeticExpression::operator,
        INumberExpression.STREAM_CODEC,
        ArithmeticExpression::left,
        INumberExpression.STREAM_CODEC,
        ArithmeticExpression::right,
        ArithmeticExpression::new
    );

    public static ArithmeticExpression of(Operator operator, double left, double right) {
        return new ArithmeticExpression(operator, ConstantExpression.of(left), ConstantExpression.of(right));
    }

    @Override
    public double evaluate(NumberArguments inputs) {
        return this.operator.apply(this.left.evaluate(inputs), this.right.evaluate(inputs));
    }

    @Override
    public Type type() {
        return ModNumberExpressionTypes.ARITHMETIC.get();
    }

    public enum Operator implements StringRepresentable {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE;

        public static final Codec<Operator> CODEC = StringRepresentable.fromEnum(Operator::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Operator> STREAM_CODEC = ByteBufCodecs
            .fromCodec(CODEC)
            .cast();

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public double apply(double left, double right) {
            return switch (this) {
                case ADD -> left + right;
                case SUBTRACT -> left - right;
                case MULTIPLY -> left * right;
                case DIVIDE -> left / right;
            };
        }
    }

    public static class Type implements INumberExpression.Type<ArithmeticExpression> {
        @Override
        public MapCodec<ArithmeticExpression> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ArithmeticExpression> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
