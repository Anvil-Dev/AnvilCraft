package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class CfaFluidInterfaceTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof CelestialForgingAnvilFluidInterfaceBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (!(value instanceof CelestialForgingAnvilFluidInterfaceBlockEntity fluid)) return List.of();
        List<Component> lines = new ArrayList<>();

        boolean hasAny = false;
        for (int i = 0; i < fluid.getTanks().length; i++) {
            var tank = fluid.getTanks()[i];
            var fluidStack = tank.getFluid();
            if (!fluidStack.isEmpty()) {
                hasAny = true;
                lines.add(Component.literal(" · ")
                    .append(fluidStack.getHoverName())
                    .append(Component.literal(" " + (fluidStack.getAmount() / 1000) + " B"))
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        if (!hasAny) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.interface.empty")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    @Override
    public ItemStack icon(BlockEntity value) {
        return ItemStack.EMPTY;
    }

    @Override
    public int priority() {
        return -1; // Higher priority than PowerComponentTooltipProvider
    }
}
