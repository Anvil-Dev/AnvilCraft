package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** 铁砧锤按原料、产物与液层显示大型炼药锅的库存和加工预览。 */
public class LargeCauldronTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof LargeCauldronBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        return CauldronProcessingTooltip.contents(value);
    }

    @Override
    public int priority() {
        return -1;
    }
}
