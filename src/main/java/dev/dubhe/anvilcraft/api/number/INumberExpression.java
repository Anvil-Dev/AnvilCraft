package dev.dubhe.anvilcraft.api.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 不依赖随机数的数学表达式，用于在配方中声明数量。
 *
 * <p>既可以直接写一个数字，也可以写成由四则运算与函数组成的表达式，还可以写 flat 表达式文本，
 * 例如 {@code "x*2"}、{@code "2x"}、{@code "$(cost)*2"}；半 flat 形式则是在 {@code expression}
 * 之外再带一个 {@code arguments} 字段，为表达式绑定具名传入值。</p>
 */
public interface INumberExpression {
    Codec<INumberExpression> TYPED_CODEC = Codec.lazyInitialized(() -> ModRegistries.NUMBER_EXPRESSION_TYPE
        .byNameCodec().dispatch(INumberExpression::type, Type::codec));
    Codec<INumberExpression> CODEC = Codec.lazyInitialized(() -> Codec.either(
        INumberExpression.TYPED_CODEC,
        INumberExpression.inlineCodec()
    ).xmap(
        Either::unwrap,
        expression -> expression instanceof ConstantExpression || expression instanceof FlatExpression
                      ? Either.right(expression)
                      : Either.left(expression)
    ));
    StreamCodec<RegistryFriendlyByteBuf, INumberExpression> STREAM_CODEC = ByteBufCodecs
        .registry(ModRegistryKeys.NUMBER_EXPRESSION_TYPE)
        .dispatch(INumberExpression::type, Type::streamCodec);

    /**
     * 计算表达式的值。除零、负数开方、下标越界、名字未绑定等情况不会抛出异常。
     *
     * @param inputs 传入值，{@link InputExpression} 按下标引用，{@link NamedExpression} 按名字引用
     */
    double evaluate(NumberArguments inputs);

    /**
     * 计算表达式的值，传入值只按下标引用。
     */
    default double evaluate(double... inputs) {
        return this.evaluate(NumberArguments.of(inputs));
    }

    /**
     * 计算表达式的值并四舍五入为整数。
     */
    default int evaluateInt(NumberArguments inputs) {
        return (int) Math.round(this.evaluate(inputs));
    }

    /**
     * 计算表达式的值并四舍五入为整数，传入值只按下标引用。
     */
    default int evaluateInt(double... inputs) {
        return this.evaluateInt(NumberArguments.of(inputs));
    }

    Type<? extends INumberExpression> type();

    /**
     * 内联数字、flat 表达式文本与半 flat 形式。
     */
    private static Codec<INumberExpression> inlineCodec() {
        return Codec.either(
            ConstantExpression.INLINE_CODEC,
            Codec.either(FlatExpression.CODEC.codec(), FlatExpression.STRING_CODEC)
        ).xmap(
            nested -> nested.map(constant -> (INumberExpression) constant, Either::unwrap),
            INumberExpression::toInline
        );
    }

    /**
     * 把内联数字、flat 表达式文本与半 flat 形式拆到对应的编码分支。
     */
    private static Either<ConstantExpression, Either<FlatExpression, FlatExpression>> toInline(
        INumberExpression expression
    ) {
        if (expression instanceof ConstantExpression constant) return Either.left(constant);
        if (expression instanceof FlatExpression flat) {
            return flat.arguments().isEmpty() ? Either.right(Either.right(flat)) : Either.right(Either.left(flat));
        }
        throw new IllegalArgumentException("Expression " + expression + " cannot be written inline");
    }

    interface Type<T extends INumberExpression> extends ISerializer<T> {
    }
}
