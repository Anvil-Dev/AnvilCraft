package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class HeatableBlockTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof HeatableBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity entity) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return null;
        List<Component> components = new ArrayList<>();
        HeatRecorder.getTier(entity.getLevel(), entity.getBlockPos(), entity.getBlockState())
            .ifPresent(tier -> components.add(ITooltipProvider.withIndentAndMerge(Component.translatable(
                "tooltip.anvilcraft.heat.tier",
                tier.getDisplayName()
            ).withStyle(ChatFormatting.GRAY))));
        Util.castSafely(entity, HeatableBlockEntity.class)
            .map(HeatableBlockEntity::getDuration)
            .ifPresent(duration -> components.add(ITooltipProvider.withIndentAndMerge(Component.translatable(
                "tooltip.anvilcraft.heat.duration",
                FormattingUtil.toFormattedTime(Math.max(duration, 0), 1)
            ).withStyle(ChatFormatting.GRAY))));
        if (!components.isEmpty()) {
            components.addFirst(Component.translatable("tooltip.anvilcraft.heat.title").withStyle(ChatFormatting.BLUE));
        }
        return components;
    }

    @Override
    public int priority() {
        return 0;
    }
}
