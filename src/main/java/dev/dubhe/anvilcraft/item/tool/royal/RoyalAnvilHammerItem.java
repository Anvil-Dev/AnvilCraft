package dev.dubhe.anvilcraft.item.tool.royal;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.world.level.block.Block;

public class RoyalAnvilHammerItem extends AnvilHammerItem {
    /// 初始化铁砧锤
    ///
    /// @param properties 物品属性
    public RoyalAnvilHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 7;
    }

    @Override
    public Block getAnvil() {
        return ModBlocks.ROYAL_ANVIL.get();
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(80, fallDistance * 2);
    }
}
