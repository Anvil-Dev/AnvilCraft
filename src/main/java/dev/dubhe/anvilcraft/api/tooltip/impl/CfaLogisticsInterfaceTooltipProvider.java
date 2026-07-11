package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

public class CfaLogisticsInterfaceTooltipProvider
    extends CfaInterfaceTooltipProvider<CelestialForgingAnvilLogisticsInterfaceBlockEntity> {
    public CfaLogisticsInterfaceTooltipProvider() {
        super(CelestialForgingAnvilLogisticsInterfaceBlockEntity.class);
    }

    @Override
    protected List<Component> buildTooltip(CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics) {
        List<Component> lines = new ArrayList<>();

        // 显示尚未完成的神庙供奉需求。
        ItemStack demandItem = logistics.getTempleDemandItem();
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

        // 显示锻星砧控制器推送的对撞机状态。
        List<ItemStack> colliderTargets = logistics.getColliderTargetItems();
        if (!colliderTargets.isEmpty() || logistics.isColliderProcessing() || logistics.isColliderStarMissing()) {
            // 缺少恒星时优先显示警告，不显示目标物品。
            if (logistics.isColliderStarMissing()) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_star_missing")
                    .withStyle(ChatFormatting.RED));
                lines.add(Component.literal(""));
            } else if (logistics.isColliderProcessing()) {
                var level = logistics.getLevel();
                int dots = level != null ? (int) ((level.getGameTime() / 10) % 3) : 0;
                String base = Component.translatable("screen.anvilcraft.cfa.collider_processing").getString();
                lines.add(Component.literal(base + ".".repeat(dots + 1) + "◇")
                    .withStyle(ChatFormatting.AQUA));
            } else {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_targets")
                    .withStyle(ChatFormatting.AQUA));
            }
            // 仅在空闲且恒星存在时显示目标物品。
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

        // 显示接口内已存储的物品。
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
    public ItemStack icon(BlockEntity value) {
        return ItemStack.EMPTY;
    }

}
