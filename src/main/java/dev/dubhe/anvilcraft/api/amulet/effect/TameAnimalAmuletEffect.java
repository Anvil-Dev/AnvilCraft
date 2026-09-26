package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 交互时驯服指定动物的护符效果
public record TameAnimalAmuletEffect(List<EntityTypePredicate> animals) implements IAmuletEffect {
    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        Entity target = ctx.get(ModAmuletEffectContextKeys.INTERACT_TARGET).orElse(null);
        if (!(target instanceof TamableAnimal animal) || animal.isTame()) {
            return;
        }
        boolean matched = false;
        for (EntityTypePredicate predicate : this.animals) {
            if (predicate.matches(animal.getType())) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return;
        }
        ctx.set(ModAmuletEffectContextKeys.HANDLE_INTERACT, true);
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (animal instanceof Wolf wolf) {
            wolf.stopBeingAngry();
        }
        animal.tame(serverPlayer);
        animal.getNavigation().stop();
        animal.setTarget(null);
        animal.setOrderedToSit(true);
        serverPlayer.level().broadcastEntityEvent(animal, EntityEvent.TAMING_SUCCEEDED);
    }
}
