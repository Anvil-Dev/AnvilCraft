package dev.dubhe.anvilcraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class RoyalHoeItem extends HoeItem implements IHasDefaultEnchantment {
    public RoyalHoeItem(Properties properties) {
        super(Tiers.DIAMOND, -3, 0, properties);
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
