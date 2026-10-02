package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.tooltip.StoragePortItemTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public class StoragePortBlockItem extends BlockItem {
    public StoragePortBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return StoragePortItemTooltip.storagePortTooltipImage(stack);
    }
}
