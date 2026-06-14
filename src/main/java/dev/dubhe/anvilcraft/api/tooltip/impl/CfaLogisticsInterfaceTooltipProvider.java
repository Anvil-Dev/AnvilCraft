package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

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

        // Show temple demand if active and unsatisfied (pushed directly by CFA controller)
        var demandItem = logistics.getTempleDemandItem();
        if (!demandItem.isEmpty() && !logistics.isTempleDemandSatisfied()) {
            lines.add(Component.literal("⛧ Temple Demand ⛧")
                .withStyle(ChatFormatting.GOLD));
            lines.add(Component.literal(" · ")
                .append(demandItem.getHoverName())
                .append(Component.literal(" ×" + logistics.getTempleDemandCount()))
                .withStyle(ChatFormatting.YELLOW));
            lines.add(Component.literal(""));
        }

        var handler = logistics.getItemHandler();
        boolean hasAny = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                hasAny = true;
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
    public ItemStack icon(BlockEntity value) {
        return ItemStack.EMPTY;
    }

    @Override
    public int priority() {
        return -1;
    }
}
