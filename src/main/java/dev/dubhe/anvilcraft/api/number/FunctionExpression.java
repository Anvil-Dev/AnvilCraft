package dev.dubhe.anvilcraft.api.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModNumberExpressionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

/**
 * 函数表达式，参数个数由函数自身决定。
 */
public record FunctionExpression(Function function, List<INumberExpression> arguments) implements INumberExpression {
    public static final MapCodec<FunctionExpression> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Function.CODEC
            .fieldOf("function")
            .forGetter(FunctionExpression::function),
        INumberExpression.CODEC
            .listOf()
            .fieldOf("arguments")
            .forGetter(FunctionExpression::arguments)
    ).apply(ins, FunctionExpression::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FunctionExpression> STREAM_CODEC = StreamCodec.composite(
        Function.STREAM_CODEC,
        FunctionExpression::function,
        INumberExpression.STREAM_CODEC.apply(ByteBufCodecs.list()),
        FunctionExpression::arguments,
        FunctionExpression::new
    );

    public FunctionExpression {
        arguments = List.copyOf(arguments);
        function.checkArguments(arguments.size());
    }

    public static FunctionExpression of(Function function, INumberExpression... arguments) {
        return new FunctionExpression(function, List.of(arguments));
    }

    @Override
    public double evaluate(NumberArguments inputs) {
        return this.function.apply(this.arguments, inputs);
    }

    @Override
    public Type type() {
        return ModNumberExpressionTypes.FUNCTION.get();
    }

    public enum Function implements StringRepresentable {
        ABS(1, 1),
        FLOOR(1, 1),
        CEIL(1, 1),
        ROUND(1, 1),
        SQRT(1, 1),
        POW(2, 2),
        MIN(1, Integer.MAX_VALUE),
        MAX(1, Integer.MAX_VALUE);

        public static final Codec<Function> CODEC = StringRepresentable.fromEnum(Function::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Function> STREAM_CODEC = ByteBufCodecs
            .fromCodec(CODEC)
            .cast();

        private final int minArguments;
        private final int maxArguments;

        Function(int minArguments, int maxArguments) {
            this.minArguments = minArguments;
            this.maxArguments = maxArguments;
        }

        /**
         * 校验参数个数，不合法时抛出异常以便配方加载阶段报错。
         */
        public void checkArguments(int size) {
            if (size < this.minArguments || size > this.maxArguments) {
                throw new IllegalArgumentException(
                    "Function %s requires %s arguments but got %s".formatted(this.getSerializedName(), this.range(), size)
                );
            }
        }

        public double apply(List<INumberExpression> arguments, NumberArguments inputs) {
            return switch (this) {
                case ABS -> Math.abs(arguments.getFirst().evaluate(inputs));
                case FLOOR -> Math.floor(arguments.getFirst().evaluate(inputs));
                case CEIL -> Math.ceil(arguments.getFirst().evaluate(inputs));
                case ROUND -> Math.round(arguments.getFirst().evaluate(inputs));
                case SQRT -> Math.sqrt(arguments.getFirst().evaluate(inputs));
                case POW -> Math.pow(arguments.get(0).evaluate(inputs), arguments.get(1).evaluate(inputs));
                case MIN -> arguments.stream().mapToDouble(argument -> argument.evaluate(inputs)).min().orElse(0);
                case MAX -> arguments.stream().mapToDouble(argument -> argument.evaluate(inputs)).max().orElse(0);
            };
        }

        private String range() {
            if (this.minArguments == this.maxArguments) return String.valueOf(this.minArguments);
            if (this.maxArguments == Integer.MAX_VALUE) return this.minArguments + " or more";
            return this.minArguments + " to " + this.maxArguments;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static class Type implements INumberExpression.Type<FunctionExpression> {
        @Override
        public MapCodec<FunctionExpression> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FunctionExpression> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
