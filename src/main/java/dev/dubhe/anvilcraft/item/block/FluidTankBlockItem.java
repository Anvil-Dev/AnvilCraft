package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/// 流体储罐的物品形态，额外显示罐内流体
public class FluidTankBlockItem extends BlockItem {
    public FluidTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
        FluidTankItemTooltip.appendExpandableTank(
            stack,
            context,
            builder,
            FluidTankBlockEntity.BASE_CAPACITY,
            FluidTankBlockEntity.INFINITY_THRESHOLD
        );
    }
}
