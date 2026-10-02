package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public class StorageFluidPortBlockItem extends BlockItem {
    public StorageFluidPortBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return FluidTankItemTooltip.singleFluidTooltipImage(stack, StorageFluidPortBlockEntity.CAPACITY_MB,
            StorageFluidPortBlockEntity.CAPACITY_MB);
    }
}
