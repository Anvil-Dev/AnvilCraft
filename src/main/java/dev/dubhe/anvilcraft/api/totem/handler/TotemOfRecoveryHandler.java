package dev.dubhe.anvilcraft.api.totem.handler;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.RecoveryPearl;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.EffectCures;

import java.util.Optional;

public class TotemOfRecoveryHandler implements TotemHandler {
    @Override
    public boolean execute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem) {
        if (!damageSource.is(DamageTypes.GENERIC_KILL)) {
            if (entity instanceof ServerPlayer player) {
                player.fallDistance = 0;
                player.awardStat(Stats.ITEM_USED.get(ModItems.TOTEM_OF_RECOVERY.get()), 1);
                CriteriaTriggers.USED_TOTEM.trigger(player, ModItems.TOTEM_OF_RECOVERY.asStack());
                entity.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                player.getInventory().add(ModItems.RECOVERY_PEARL.asStack());
                ResourceKey<Level> deathDimension = player.level().dimension();
                BlockPos deathPos = player.getOnPos();
                if (deathDimension == Level.OVERWORLD) {
                    if (deathPos.getY() < -64) {
                        deathPos = deathPos.atY(-63);
                    }
                } else {
                    if (deathPos.getY() < 0) {
                        deathPos = deathPos.atY(1);
                    }
                }
                deathPos = deathPos.atY(deathPos.getY() + 1);
                player.setLastDeathLocation(Optional.of(GlobalPos.of(deathDimension, deathPos)));
                ResourceKey<Level> respawnDimension = player.getRespawnDimension();
                BlockPos respawnPos = player.getRespawnPosition() == null ? player.level().getSharedSpawnPos() : player.getRespawnPosition();
                RecoveryPearl.crossDimensionTeleportTo(respawnDimension, player, respawnPos);
            }
            entity.setHealth(1.0f);
            entity.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            entity.level().broadcastEntityEvent(entity, (byte) 36);

            return true;
        }
        return false;
    }

    @Override
    public void shrink(ItemStack totemItem) {
        totemItem.shrink(1);
    }
}
