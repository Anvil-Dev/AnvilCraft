package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class AmethystHoeItem extends HoeItem {
    public AmethystHoeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, -1, -2.0f)));
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(level.registryAccess().holderOrThrow(ModEnchantments.HARVEST_KEY), 1);
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
    }
}
