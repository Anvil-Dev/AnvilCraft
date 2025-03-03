package dev.dubhe.anvilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class AmethystShovelItem extends ShovelItem {
    public AmethystShovelItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 1.5f, -3.0f)));
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(level.registryAccess().holderOrThrow(Enchantments.EFFICIENCY), 3);
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
    }
}
