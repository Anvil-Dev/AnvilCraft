package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 超重物品
 */
public interface ISuperHeavy extends IAbnormal {
    @Override
    default void addEffect(Player player) {
        int count = this.getItemCount(player);
        int amplifier = 0;
        if (count > 64) amplifier = 3;
        else if (count > 16) amplifier = 2;
        else if (count > 4) amplifier = 1;
        player.addEffect(IAbnormal.makeEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, amplifier));
    }
}
