package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 有默认附魔
 */
public interface IHasDefaultEnchantment {
    Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments();

    default int getDefaultEnchantmentLevel(Enchantment enchantment) {
        return this.getDefaultEnchantments().getOrDefault(enchantment, 0);
    }

    /**
     * @return 工具提示
     */
    default List<Component> getDefaultEnchantmentsTooltip() {
        List<Component> list = new ArrayList<>();
        list.add(
            Component
                .translatable("item.anvilcraft.default_enchantment.tooltip")
                .withStyle(ChatFormatting.GRAY)
        );
        for (Map.Entry<ResourceKey<Enchantment>, Integer> entry : getDefaultEnchantments().entrySet()) {
            ResourceKey<Enchantment> enchantment = entry.getKey();
            Integer level = entry.getValue();
            list.add(
                Component
                    .literal("- ")
                    .append(Enchantment.getFullname(level))
                    .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
        return list;
    }
}
