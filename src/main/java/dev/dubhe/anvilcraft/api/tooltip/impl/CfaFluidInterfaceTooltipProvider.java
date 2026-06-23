package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

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
        ResourceHandler<FluidResource> handler = fluid.getFluidHandler();
        boolean hasAny = false;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource resource = handler.getResource(i);
            if (!resource.isEmpty()) {
                hasAny = true;
                FluidStack stack = resource.toStack((int) handler.getAmountAsLong(i));
                lines.add(Component.literal(" · ")
                    .append(stack.getHoverName())
                    .append(Component.literal(" " + (stack.getAmount() / 1000) + " B"))
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
    public int priority() {
        return -1;
    }
}
