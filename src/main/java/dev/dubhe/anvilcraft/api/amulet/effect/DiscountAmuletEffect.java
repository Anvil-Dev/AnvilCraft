package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/// 提供村民交易折扣的护符效果。<br>
/// 折扣率由表达式求值得到，表达式的第一个输入 {@code x} 是已有的折扣率（没有折扣时为 1），
/// 因此 {@code 0.3} 表示折扣率为 0.3、{@code x*0.8} 表示在已有折扣上再乘 0.8。
public record DiscountAmuletEffect(IExpression rate) implements IAmuletEffect {
    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        float current = ctx.get(ModAmuletEffectContextKeys.DISCOUNT_RATE).orElse(1F);
        ctx.set(ModAmuletEffectContextKeys.DISCOUNT_RATE, (float) this.rate.evaluate(current));
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.DISCOUNT.get();
    }

    public static class Type implements IAmuletEffect.Type<DiscountAmuletEffect> {
        public static final MapCodec<DiscountAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            IExpression.CODEC
                .fieldOf("rate")
                .forGetter(DiscountAmuletEffect::rate)
        ).apply(inst, DiscountAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DiscountAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            IExpression.STREAM_CODEC,
            DiscountAmuletEffect::rate,
            DiscountAmuletEffect::new
        );

        @Override
        public MapCodec<DiscountAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DiscountAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
