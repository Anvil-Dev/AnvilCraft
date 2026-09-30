package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/// 进食时免疫负面药水效果的护符效果
public record ImmuneEatEffectAmuletEffect(MobEffectCategory category) implements IAmuletEffect {
    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        if (!ctx.getOrDefault(ModAmuletEffectContextKeys.CONSUMING_FOOD, false)) {
            return;
        }
        ctx.get(ModAmuletEffectContextKeys.MOB_EFFECT)
            .filter(effect -> effect.getEffect().value().getCategory() == this.category())
            .ifPresent(effect -> ctx.set(ModAmuletEffectContextKeys.IMMUNE_MOB_EFFECT, true));
    }
}
