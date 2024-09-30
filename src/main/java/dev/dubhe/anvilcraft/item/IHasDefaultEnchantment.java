package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 有默认附魔
 */
public interface IHasDefaultEnchantment {
    Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments();

    /**
     * @return 工具提示
     */
    default List<Component> getDefaultEnchantmentsTooltip(Level level) {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("item.anvilcraft.default_enchantment.tooltip")
            .withStyle(ChatFormatting.GRAY));

        for (var entry : getDefaultEnchantments().entrySet()) {
            Holder<Enchantment> enchantmentHolder = level.registryAccess().holderOrThrow(entry.getKey());
            int l = entry.getValue();
            list.add(
                Component
                    .literal("- ")
                    .append(Enchantment.getFullname(enchantmentHolder, l))
                    .withStyle(ChatFormatting.DARK_GRAY)
            );
        }

        return list;
    }
}
