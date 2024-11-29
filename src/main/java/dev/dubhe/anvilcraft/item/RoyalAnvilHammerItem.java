package dev.dubhe.anvilcraft.item;

public class RoyalAnvilHammerItem extends AnvilHammerItem {
    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public RoyalAnvilHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 7;
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(80, fallDistance * 2);
    }
}
