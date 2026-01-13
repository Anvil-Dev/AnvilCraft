package dev.dubhe.anvilcraft.inventory.tooltip;

import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FilterTooltip(FilterContent content) implements TooltipComponent {
}
