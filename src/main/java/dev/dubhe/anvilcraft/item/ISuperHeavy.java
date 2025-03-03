package dev.dubhe.anvilcraft.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 超重物品
 */
public interface ISuperHeavy {
    /**
     * 统计超重物品数量
     *
     * @param player 玩家
     * @return 超重物品数量
     */
    static int hasSuperHeavyNumber(@NotNull Player player) {
        Inventory inventory = player.getInventory();
        int i = 0;
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!(itemStack.getItem() instanceof ISuperHeavy)) continue;
            i += itemStack.getCount();
        }
        return i;
    }

    /**
     * 执行效果
     *
     * @param stack      物品
     * @param level      世界
     * @param entity     实体
     * @param slotId     槽位id
     * @param isSelected 是否选中
     */
    default void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;
        if (player.getAbilities().instabuild || player.getAbilities().invulnerable) return;
        int superHeavyItemCount = hasSuperHeavyNumber(player);
        int amplifier = 0;
        if (superHeavyItemCount > 64) amplifier = 3;
        else if (superHeavyItemCount > 16) amplifier = 2;
        else if (superHeavyItemCount > 4) amplifier = 1;
        MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, amplifier, false, true);
        player.addEffect(slowness);
    }
}
