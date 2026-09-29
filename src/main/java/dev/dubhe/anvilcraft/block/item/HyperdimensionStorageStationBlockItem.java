package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.item.ICannotFitInStationItem;
import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class HyperdimensionStorageStationBlockItem extends SimpleMultiPartBlockItem<Cube3x3PartHalf> implements ICannotFitInStationItem {
    public HyperdimensionStorageStationBlockItem(HyperdimensionStorageStationBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return ItemTooltipManager.getStorageTooltip(stack);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
