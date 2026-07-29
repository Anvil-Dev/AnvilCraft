package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.world.level.block.Block;

public class FrostAnvilHammerItem extends AnvilHammerItem {
    public FrostAnvilHammerItem(Properties properties) {
        super(properties.component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 9;
    }

    @Override
    public Block getAnvil() {
        return ModBlocks.FROST_ANVIL.get();
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(120, fallDistance * 2);
    }
}
