package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 有默认附魔
 */
public interface HasDefaultEnchantment {
    Map<Enchantment, Integer> getDefaultEnchantments();

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
        for (Map.Entry<Enchantment, Integer> entry : getDefaultEnchantments().entrySet()) {
            Enchantment enchantment = entry.getKey();
            Integer level = entry.getValue();
            list.add(
                Component
                    .literal("- ")
                    .append(enchantment.getFullname(level))
                    .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
        return list;
    }
}
