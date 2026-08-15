package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record StorageTooltip(int usedTypes, int typeLimit, List<ItemStack> types) implements TooltipComponent {
}
