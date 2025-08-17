package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 辐射物品
 */
public interface IRadiation extends IAbnormal {
    @Override
    default void addEffect(Player player) {
        int count = this.getItemCount(player);
        if (count < 1152) return;
        player.addEffect(IAbnormal.makeEffectInstance(MobEffects.WITHER, 0));
    }
}
