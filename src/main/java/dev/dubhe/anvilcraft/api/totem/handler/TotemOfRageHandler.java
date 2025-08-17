package dev.dubhe.anvilcraft.api.totem.handler;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMobEffects;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

public class TotemOfRageHandler implements TotemHandler {
    @Override
    public boolean execute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (entity instanceof ServerPlayer player) {
                player.getFoodData().setFoodLevel(20);
                CriteriaTriggers.USED_TOTEM.trigger(player, ModItems.TOTEM_OF_RAGE.asStack());
                entity.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            entity.setHealth(entity.getMaxHealth());
            entity.removeAllEffects();
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 4));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 2));
            entity.addEffect(new MobEffectInstance(ModMobEffects.INVULNERABLE, 1200, 0));
            entity.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 0));
            entity.level().broadcastEntityEvent(entity, (byte) 37);

            return true;
        }
        return false;
    }

    @Override
    public void shrink(ItemStack totemItem) {
        totemItem.shrink(1);
    }
}
