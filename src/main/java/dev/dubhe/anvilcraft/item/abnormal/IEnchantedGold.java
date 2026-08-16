package dev.dubhe.anvilcraft.item.abnormal;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 附魔金物品
 */
public interface IEnchantedGold {
    default void inventoryTick(ItemStack ignored, Level level, Entity entity, int ignored1, boolean ignored2) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (player.getAbilities().instabuild || player.getAbilities().invulnerable) return;
        if (AmuletManager.get(level.registryAccess()).hasAmuletInInventory(player, ModAmulets.ABNORMAL)) return;
        if (getEnchantedGoldCount(player) >= 64) {
            player.addEffect(IAbnormal.makeEffectInstance(MobEffects.LUCK, 0));
        }
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.HUNGER);
    }

    /**
     * 玩家是否手持附魔金
     */
    static boolean isHoldingEnchantedGold(Player player) {
        return player.isHolding(stack -> stack.is(ModItemTags.ENCHANTED_GOLD));
    }

    /**
     * 玩家是否携带附魔金
     */
    static boolean isCarryingEnchantedGold(Player player) {
        return getEnchantedGoldCount(player) > 0;
    }

    /**
     * 统计玩家携带的附魔金数量
     */
    static int getEnchantedGoldCount(Player player) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.is(ModItemTags.ENCHANTED_GOLD)) continue;
            count += itemStack.getCount();
        }
        return count;
    }
}
