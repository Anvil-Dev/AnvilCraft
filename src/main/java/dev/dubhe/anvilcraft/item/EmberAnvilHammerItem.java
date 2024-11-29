package dev.dubhe.anvilcraft.item;

public class EmberAnvilHammerItem extends AnvilHammerItem implements IFireReforging {
    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public EmberAnvilHammerItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 9;
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(120, fallDistance * 2);
    }
}
