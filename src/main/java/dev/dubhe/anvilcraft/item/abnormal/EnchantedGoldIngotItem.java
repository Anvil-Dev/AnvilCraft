package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.item.ItemStack;

public class EnchantedGoldIngotItem extends EnchantedGoldItem {
    public EnchantedGoldIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isPiglinCurrency(ItemStack stack) {
        return true;
    }
}
