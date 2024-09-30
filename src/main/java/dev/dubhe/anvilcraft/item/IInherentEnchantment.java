package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 有默认附魔
 */
public interface IInherentEnchantment {
    Map<ResourceKey<Enchantment>, Integer> getInherentEnchantments();

    /**
     * @return 工具提示
     */
    default List<Component> getInherentEnchantmentsTooltip(Level level) {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("item.anvilcraft.inherent_enchantment.tooltip")
            .withStyle(ChatFormatting.LIGHT_PURPLE));

        for (var entry : getInherentEnchantments().entrySet()) {
            Holder<Enchantment> enchantmentHolder = level.registryAccess().holderOrThrow(entry.getKey());
            int l = entry.getValue();
            list.add(
                Component
                    .literal("- ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(getFullname(enchantmentHolder, l))
            );
        }

        return list;
    }

    static Component getFullname(Holder<Enchantment> enchantment, int level) {
        MutableComponent component = enchantment.value().description().copy();
        ComponentUtils.mergeStyles(component, Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE));
        if (level != 1 || enchantment.value().getMaxLevel() != 1) {
            component.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        }
        return component;
    }
}
