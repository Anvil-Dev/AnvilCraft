package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
