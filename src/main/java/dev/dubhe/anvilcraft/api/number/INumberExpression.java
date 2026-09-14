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
 * <p>既可以直接写一个数字，也可以写成由四则运算与函数组成的表达式。</p>
 */
public interface INumberExpression {
    Codec<INumberExpression> TYPED_CODEC = Codec.lazyInitialized(() -> ModRegistries.NUMBER_EXPRESSION_TYPE
        .byNameCodec().dispatch(INumberExpression::type, Type::codec));
    Codec<INumberExpression> CODEC = Codec.lazyInitialized(() -> Codec.either(
        INumberExpression.TYPED_CODEC,
        ConstantExpression.INLINE_CODEC
    ).xmap(
        Either::unwrap,
        expression -> expression instanceof ConstantExpression constant
                      ? Either.right(constant)
                      : Either.left(expression)
    ));
    StreamCodec<RegistryFriendlyByteBuf, INumberExpression> STREAM_CODEC = ByteBufCodecs
        .registry(ModRegistryKeys.NUMBER_EXPRESSION_TYPE)
        .dispatch(INumberExpression::type, Type::streamCodec);

    /**
     * 计算表达式的值。除零、负数开方、下标越界等情况不会抛出异常。
     *
     * @param inputs 传入值，可以传入一个或多个，由 {@link InputExpression} 按下标引用
     */
    double evaluate(double... inputs);

    /**
     * 计算表达式的值并四舍五入为整数。
     */
    default int evaluateInt(double... inputs) {
        return (int) Math.round(this.evaluate(inputs));
    }

    Type<? extends INumberExpression> type();

    interface Type<T extends INumberExpression> extends ISerializer<T> {
    }
}
