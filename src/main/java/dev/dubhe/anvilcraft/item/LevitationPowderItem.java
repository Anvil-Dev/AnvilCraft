package dev.dubhe.anvilcraft.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class LevitationPowderItem extends Item {

    public LevitationPowderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(
        @NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected
    ) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
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

    private static MobEffectInstance getEffectInstance(Holder<MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, 200, amplifier, true, true);
    }

    /**
     * 统计漂浮粉末数量
     *
     * @param player 玩家
     * @return 诅咒物品数量
     */
    private int hasItemNumber(@NotNull Player player) {
        Inventory inventory = player.getInventory();
        int i = 0;
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!(itemStack.is(this))) continue;
            i += itemStack.getCount();
        }
        return i;
    }
}
