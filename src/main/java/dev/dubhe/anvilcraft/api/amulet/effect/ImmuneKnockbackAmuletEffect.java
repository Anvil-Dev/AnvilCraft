package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/// 免疫击退的护符效果
public record ImmuneKnockbackAmuletEffect() implements IAmuletEffect {
    public static final ImmuneKnockbackAmuletEffect INSTANCE = new ImmuneKnockbackAmuletEffect();

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IMMUNE_KNOCKBACK, true);
    }
}
