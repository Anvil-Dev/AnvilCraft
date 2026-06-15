package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class CreativeFluidTankTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity be) {
        return be instanceof CreativeFluidTankBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity be) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();

        final List<Component> lines = new ArrayList<>();
        if (be instanceof IFluidHandlerHolder tank) {
            IFluidHandler fluidHandler = tank.getFluidHandler();
            FluidStack fluidInTank = fluidHandler.getFluidInTank(0);
            if (!fluidInTank.isEmpty()) {
                lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
                lines.add(Component.literal("  ").append(tank.getFluidHandler().getFluidInTank(0).getHoverName())
                    .append(" × ")
                    .append(Component.translatable("tooltip.anvilcraft.infinity"))
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
