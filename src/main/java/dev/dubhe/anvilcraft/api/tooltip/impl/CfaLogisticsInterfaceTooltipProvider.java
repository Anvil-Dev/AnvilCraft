package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

public class CfaLogisticsInterfaceTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (!(value instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics)) return List.of();
        List<Component> lines = new ArrayList<>();

        ResourceHandler<ItemResource> handler = logistics.getItemHandler();
        boolean hasAny = false;
        for (int i = 0; i < handler.size(); i++) {
            ItemResource resource = handler.getResource(i);
            if (!resource.isEmpty()) {
                hasAny = true;
                ItemStack stack = resource.toStack(handler.getAmountAsInt(i));
                lines.add(Component.literal(" · ")
                    .append(stack.getHoverName())
                    .append(Component.literal(" ×" + stack.getCount()))
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
