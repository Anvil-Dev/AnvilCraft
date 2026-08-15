package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Optional;

public class ShulkerContainerBlockItem extends FlexibleMultiPartBlockItem<OpenedCube3x3PartHalf, BooleanProperty, Boolean> {
    public ShulkerContainerBlockItem(ShulkerContainerBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return ItemTooltipManager.getStorageTooltip(stack);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
