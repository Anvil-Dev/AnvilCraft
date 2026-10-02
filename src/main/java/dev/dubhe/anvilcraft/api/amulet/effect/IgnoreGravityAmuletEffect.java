package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/// 无视天体引力的护符效果
public record IgnoreGravityAmuletEffect() implements IAmuletEffect {
    public static final IgnoreGravityAmuletEffect INSTANCE = new IgnoreGravityAmuletEffect();

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IGNORE_GRAVITY, true);
    }
}
