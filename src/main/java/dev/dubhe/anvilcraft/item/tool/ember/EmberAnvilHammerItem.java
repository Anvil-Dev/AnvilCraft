package dev.dubhe.anvilcraft.item.tool.ember;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Block;

public class EmberAnvilHammerItem extends AnvilHammerItem {
    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public EmberAnvilHammerItem(Properties properties) {
        super(
            properties.fireResistant()
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 9;
    }

    @Override
    public Block getAnvil() {
        return ModBlocks.EMBER_ANVIL.get();
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(120, fallDistance * 2);
    }
}
