package dev.dubhe.anvilcraft.api.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModNumberExpressionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * flat 表达式，形如 {@code x*2}、{@code 2x} 的文本表达式，可以附带具名传入值的绑定。
 *
 * @param expression flat 表达式文本
 * @param arguments  为表达式绑定的具名传入值，在表达式中用 {@code $(name)} 引用
 */
public record FlatExpression(String expression, Map<String, INumberExpression> arguments) implements INumberExpression {
    private static final Codec<String> EXPRESSION_CODEC = Codec.STRING.comapFlatMap(
        FlatExpressionParser::validate,
        expression -> expression
    );
    public static final MapCodec<FlatExpression> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        EXPRESSION_CODEC
            .fieldOf("expression")
            .forGetter(FlatExpression::expression),
        Codec.unboundedMap(Codec.STRING, INumberExpression.CODEC)
            .optionalFieldOf("arguments", Map.of())
            .forGetter(FlatExpression::arguments)
    ).apply(ins, FlatExpression::new));
    public static final Codec<FlatExpression> STRING_CODEC = Codec.STRING.comapFlatMap(
        expression -> FlatExpressionParser.validate(expression).map(valid -> new FlatExpression(valid, Map.of())),
        FlatExpression::expression
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FlatExpression> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        FlatExpression::expression,
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, INumberExpression.STREAM_CODEC),
        FlatExpression::arguments,
        FlatExpression::new
    );

    public FlatExpression {
        FlatExpressionParser.parse(expression);
        arguments = Map.copyOf(arguments);
    }

    public static FlatExpression of(String expression) {
        return new FlatExpression(expression, Map.of());
    }

    @Override
    public double evaluate(NumberArguments inputs) {
        INumberExpression parsed = FlatExpressionParser.parse(this.expression);
        if (this.arguments.isEmpty()) return parsed.evaluate(inputs);
        NumberArguments scoped = inputs;
        for (Map.Entry<String, INumberExpression> entry : this.arguments.entrySet()) {
            scoped = scoped.with(entry.getKey(), entry.getValue().evaluate(inputs));
        }
        return parsed.evaluate(scoped);
    }

    @Override
    public Type type() {
        return ModNumberExpressionTypes.FLAT.get();
    }

    public static class Type implements INumberExpression.Type<FlatExpression> {
        @Override
        public MapCodec<FlatExpression> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FlatExpression> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
