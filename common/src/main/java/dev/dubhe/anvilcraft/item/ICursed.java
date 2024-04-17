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
 * 诅咒物品
 */
public interface ICursed {
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
        if (player.getAbilities().instabuild) return;
        MobEffectInstance weakness = new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true);
        MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, true);
        MobEffectInstance hungry = new MobEffectInstance(MobEffects.HUNGER, 200, 1, false, true);
        player.addEffect((weakness));
        int curedNumber = ICursed.hasCuredNumber(player);
        if (curedNumber > 8) {
            player.addEffect((slowness));
        }
        if (curedNumber > 64) {
            player.addEffect((hungry));
        }
    }

    /**
     * 统计诅咒物品数量
     *
     * @param player 玩家
     * @return 诅咒物品数量
     */
    static int hasCuredNumber(@NotNull Player player) {
        Inventory inventory = player.getInventory();
        int i = 0;
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!(itemStack.getItem() instanceof ICursed)) continue;
            i += itemStack.getCount();
        }
        return i;
    }
}
