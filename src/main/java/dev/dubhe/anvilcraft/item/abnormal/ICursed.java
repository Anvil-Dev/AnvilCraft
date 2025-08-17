package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;

/**
 * 诅咒物品
 */
public interface ICursed extends IItemExtension, IAbnormal {
    @Override
    default void addEffect(Player player) {
        player.addEffect(IAbnormal.makeEffectInstance(MobEffects.WEAKNESS, 1));
        int count = this.getItemCount(player);
        if (count > 8) {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1));
        }
        if (count > 64) {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.HUNGER, 1));
        }
    }

    @Override
    default boolean isPiglinCurrency(ItemStack stack) {
        return true;
    }
}
