package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
    public Block getAnvil() {
        return ModBlocks.ROYAL_ANVIL.get();
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(80, fallDistance * 2);
    }
}
