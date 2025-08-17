package dev.dubhe.anvilcraft.item.abnormal;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IAbnormal {
    /**
     * 执行效果
     *
     * @param level  世界
     * @param entity 实体
     */
    default void inventoryTick(ItemStack ignored, Level level, Entity entity, int ignored1, boolean ignored2) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (player.getAbilities().instabuild || player.getAbilities().invulnerable) return;
        if (AmuletManager.INSTANCE.hasAmuletInInventory(player, ModItems.ABNORMAL_AMULET)) return;
        this.addEffect(player);
    }

    void addEffect(Player player);

    static MobEffectInstance makeEffectInstance(Holder<MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, 200, amplifier, false, true);
    }

    /**
     * 统计异常物品数量
     *
     * @param player 玩家
     * @return 异常物品数量
     */
    default int getItemCount(Player player) {
        Inventory inventory = player.getInventory();
        int i = 0;
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!this.getClass().isInstance(itemStack.getItem())) continue;
            i += itemStack.getCount();
        }
        return i;
    }

    /**
     * 统计异常物品数量
     *
     * @param player 玩家
     * @return 异常物品数量
     */
    static <T extends IAbnormal> int getAbnormalCount(Player player, Class<T> clazz) {
        Inventory inventory = player.getInventory();
        int i = 0;
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!clazz.isInstance(itemStack.getItem())) continue;
            i += itemStack.getCount();
        }
        return i;
    }
}
