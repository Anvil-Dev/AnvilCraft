package dev.dubhe.anvilcraft.api.totem.handler;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.EffectCures;

public class TotemOfUndyingHandler implements TotemHandler {
    @Override
    public boolean execute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (entity instanceof ServerPlayer player) {
                player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING), 1);
                CriteriaTriggers.USED_TOTEM.trigger(player, Items.TOTEM_OF_UNDYING.getDefaultInstance());
                entity.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            entity.setHealth(1.0f);
            entity.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            entity.level().broadcastEntityEvent(entity, (byte) 35);
            return true;
        }
        return false;
    }

    @Override
    public void shrink(ItemStack totemItem) {
        totemItem.shrink(1);
    }
}
