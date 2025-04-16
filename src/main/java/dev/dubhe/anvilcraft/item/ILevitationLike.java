package dev.dubhe.anvilcraft.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface ILevitationLike<T extends Item & ILevitationLike<T>> {
    default void addEffectToPlayer(Player player) {
        int curedNumber = this.hasItemNumber(player);
        if (curedNumber < 64) return;
        if (curedNumber < 128) {
            player.addEffect(getEffectInstance(MobEffects.SLOW_FALLING, 0));
            player.addEffect(getEffectInstance(MobEffects.JUMP, 0));
        } else if (curedNumber < 192) {
            player.addEffect(getEffectInstance(MobEffects.SLOW_FALLING, 1));
            player.addEffect(getEffectInstance(MobEffects.JUMP, 1));
        } else if (curedNumber < 256) {
            player.addEffect(getEffectInstance(MobEffects.LEVITATION, 0));
        } else {
            player.addEffect(getEffectInstance(MobEffects.LEVITATION, 1));
        }
    }

    static MobEffectInstance getEffectInstance(Holder<MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, 200, amplifier, true, true);
    }

    /**
     * 统计漂浮粉末数量
     *
     * @param player 玩家
     * @return 诅咒物品数量
     */
    @SuppressWarnings("unchecked")
    default int hasItemNumber(@NotNull Player player) {
        Inventory inventory = player.getInventory();
        int i = 0;
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!(itemStack.getItem() instanceof ILevitationLike<?>)) continue;
            i += itemStack.getCount();
        }
        return i;
    }
}
