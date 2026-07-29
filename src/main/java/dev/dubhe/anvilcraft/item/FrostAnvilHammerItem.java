package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class FrostAnvilHammerItem extends AnvilHammerItem {
    public FrostAnvilHammerItem(Properties properties) {
        super(properties.component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.FROST_METAL_INGOT);
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
