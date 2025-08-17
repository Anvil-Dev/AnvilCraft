package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 飘浮物品
 */
public interface ILevitation extends IAbnormal {
    @Override
    default void addEffect(Player player) {
        int count = this.getItemCount(player);
        if (count < 64) return;
        if (count < 128) {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.SLOW_FALLING, 0));
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.JUMP, 0));
        } else if (count < 192) {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.SLOW_FALLING, 1));
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.JUMP, 1));
        } else if (count < 256) {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.LEVITATION, 0));
        } else {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.LEVITATION, 1));
        }
    }
}
