package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class FluidTankTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof FluidTankBlockEntity || value instanceof LargeFluidTankBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();

        final List<Component> lines = new ArrayList<>();
        if (value instanceof IFluidHandlerHolder tank) {
            lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity")
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            if (value instanceof LargeFluidTankBlockEntity be && be.isInfinity()) {
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value.infinity",
                        tank.getFluidHandler().getFluidInTank(0).getAmount()
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            } else {
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value",
                        tank.getFluidHandler().getFluidInTank(0).getAmount(),
                        tank.getFluidHandler().getTankCapacity(0)
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            }
        }
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
