package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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

        // Show temple demand if active and unsatisfied
        var demandItem = logistics.getTempleDemandItem();
        if (!demandItem.isEmpty() && !logistics.isTempleDemandSatisfied()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.temple_demand")
                .withStyle(ChatFormatting.GOLD));
            int progress = logistics.getTempleDemandProgress();
            if (progress > 0) {
                lines.add(Component.literal(" · ")
                    .append(demandItem.getHoverName())
                    .append(Component.literal(" " + progress + "/" + logistics.getTempleDemandCount()))
                    .withStyle(ChatFormatting.YELLOW));
            } else {
                lines.add(Component.literal(" · ")
                    .append(demandItem.getHoverName())
                    .append(Component.literal(" ×" + logistics.getTempleDemandCount()))
                    .withStyle(ChatFormatting.YELLOW));
            }
            lines.add(Component.literal(""));
        }

        // Show collider info if active (pushed by CFA controller)
        var colliderTargets = logistics.getColliderTargetItems();
        if (!colliderTargets.isEmpty() || logistics.isColliderProcessing() || logistics.isColliderStarMissing()) {
            // Star missing warning — shown instead of target items
            if (logistics.isColliderStarMissing()) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_star_missing")
                    .withStyle(ChatFormatting.RED));
                lines.add(Component.literal(""));
            } else if (logistics.isColliderProcessing()) {
                var level = Minecraft.getInstance().level;
                int dots = level != null ? (int) ((level.getGameTime() / 10) % 3) : 0;
                String base = Component.translatable("screen.anvilcraft.cfa.collider_processing").getString();
                lines.add(Component.literal(base + ".".repeat(dots + 1) + "◇")
                    .withStyle(ChatFormatting.AQUA));
            } else {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_targets")
                    .withStyle(ChatFormatting.AQUA));
            }
            // Only show target items when idle (not processing, not star missing)
            if (!logistics.isColliderStarMissing() && !logistics.isColliderProcessing()) {
                for (ItemStack target : colliderTargets) {
                    if (!target.isEmpty()) {
                        lines.add(Component.literal(" · ")
                            .append(target.getHoverName())
                            .withStyle(ChatFormatting.AQUA));
                    }
                }
            }
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
