package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class CreativeCrateTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity be) {
        return be instanceof CreativeCrateBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity be) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();

        final List<Component> lines = new ArrayList<>();
        if (be instanceof IItemHandlerHolder itemHandlerHolder) {
            IItemHandler itemHandler = itemHandlerHolder.getItemHandler();
            ItemStack stackInSlot = itemHandler.getStackInSlot(0);
            if (!stackInSlot.isEmpty()) {
                lines.add(Component.translatable("entity.minecraft.item")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
                lines.add(Component.literal("  ").append(stackInSlot.getHoverName())
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
