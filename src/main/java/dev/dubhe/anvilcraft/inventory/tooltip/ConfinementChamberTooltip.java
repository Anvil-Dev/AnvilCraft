package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * 约束仓物品 tooltip 的数据载体，携带存储的内容物。
 *
 * @param item 槽 0 中的内容物
 */
public record ConfinementChamberTooltip(ItemStack item) implements TooltipComponent {
}