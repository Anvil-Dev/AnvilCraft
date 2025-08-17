package dev.dubhe.anvilcraft.api.totem.handler;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 图腾触发时的事件
 */
public interface TotemHandler {
    /**
     * 判断图腾是否可触发
     *
     * @param damageSource 伤害来源
     * @param entity       持有图腾的实体
     * @param totemItem    图腾物品
     * @return 是否执行成功
     */
    default boolean canExecute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem) {
        return true;
    }

    /**
     * 图腾触发时执行
     *
     * @param damageSource 伤害来源
     * @param entity       持有图腾的实体
     * @param totemItem    图腾物品
     * @return 是否执行成功
     */
    boolean execute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem);

    /**
     * 减少图腾物品数量
     *
     * @param totemItem 图腾物品
     */
    void shrink(ItemStack totemItem);
}
