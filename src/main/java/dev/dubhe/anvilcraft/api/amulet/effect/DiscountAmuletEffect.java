package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/// 提供村民交易折扣的护符效果。<br>
/// 多个此效果同时生效会将折扣率相加。
public record DiscountAmuletEffect(float rate) implements IAmuletEffect {
    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        float current = ctx.getOrDefault(ModAmuletEffectContextKeys.DISCOUNT_RATE, 0F);
        ctx.set(ModAmuletEffectContextKeys.DISCOUNT_RATE, current + this.rate);
    }
}
