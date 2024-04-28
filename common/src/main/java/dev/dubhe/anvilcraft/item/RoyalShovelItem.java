package dev.dubhe.anvilcraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class RoyalShovelItem extends ShovelItem implements HasDefaultEnchantment {
    public RoyalShovelItem(Properties properties) {
        super(Tiers.DIAMOND, 1.5f, -3.0f, properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @Nullable Level level,
        @NotNull List<Component> tooltipComponents,
        @NotNull TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.addAll(this.getDefaultEnchantmentsTooltip());
    }

    @Override
    public Map<Enchantment, Integer> getDefaultEnchantments() {
        return Map.of(Enchantments.UNBREAKING, 3);
    }
}
