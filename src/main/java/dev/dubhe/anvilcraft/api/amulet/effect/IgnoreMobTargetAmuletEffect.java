package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/// 使指定生物无视佩戴者的护符效果
public record IgnoreMobTargetAmuletEffect(List<EntityTypePredicate> mobs) implements IAmuletEffect {
    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<EntityType<?>> type = ctx.get(ModAmuletEffectContextKeys.MOB_TYPE);
        if (type.isEmpty()) {
            return;
        }
        for (EntityTypePredicate mob : this.mobs) {
            if (mob.matches(type.get())) {
                ctx.set(ModAmuletEffectContextKeys.IGNORE_MOB, true);
                return;
            }
        }
    }
}
