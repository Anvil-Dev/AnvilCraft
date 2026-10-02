package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.tooltip.ConfinementChamberItemTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public class ConfinementChamberItem extends BlockItem {
    public ConfinementChamberItem(Block block, Properties properties) {
        super(block, properties.component(DataComponents.TOOLTIP_DISPLAY,
            TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true)));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return ConfinementChamberItemTooltip.confinementChamberTooltipImage(stack);
    }
}