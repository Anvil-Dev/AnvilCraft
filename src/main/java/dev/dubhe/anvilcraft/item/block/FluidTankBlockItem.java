package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/// 流体储罐的物品形态，额外显示罐内流体
public class FluidTankBlockItem extends BlockItem {
    public FluidTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return FluidTankItemTooltip.singleFluidTooltipImage(
            stack,
            FluidTankBlockEntity.BASE_CAPACITY,
            FluidTankBlockEntity.infinityThreshold()
        );
    }
}
