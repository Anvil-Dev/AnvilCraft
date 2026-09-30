package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/// 使指定生物无视佩戴者的护符效果
public record IgnoreMobTargetAmuletEffect(List<EntityTypePredicate> mobs) implements IAmuletEffect {
    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<Mob> target = ctx.get(ModAmuletEffectContextKeys.TARGETING_MOB);
        if (target.isEmpty()) {
            return;
        }
        for (EntityTypePredicate mob : this.mobs) {
            if (mob.matches(target.get().getType())) {
                ctx.set(ModAmuletEffectContextKeys.IGNORE_MOB, true);
                return;
            }
        }
    }
}
