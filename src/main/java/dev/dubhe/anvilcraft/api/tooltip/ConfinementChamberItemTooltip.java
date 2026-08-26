package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.ConfinementChamberTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Optional;

public final class ConfinementChamberItemTooltip {
    private ConfinementChamberItemTooltip() {
    }

    /** 约束仓物品的 tooltip 数据：读取 CONTAINER 组件中槽 0 的内容物。 */
    public static Optional<TooltipComponent> confinementChamberTooltipImage(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        ItemStack item = contents.copyOne();
        if (item.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConfinementChamberTooltip(item));
    }
}