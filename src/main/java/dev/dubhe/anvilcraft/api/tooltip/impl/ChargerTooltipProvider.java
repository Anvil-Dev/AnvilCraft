package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChargerTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof ChargerBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) return null;
        if (!(value instanceof ChargerBlockEntity charger)) return null;
        List<Component> lines = new ArrayList<>();
        boolean overloaded = false;
        BlockPos pos = charger.getBlockPos();
        if (charger.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
            overloaded = charger.getBlockState()
                .getValues()
                .getOrDefault(IPowerComponent.OVERLOAD, true)
                .equals(Boolean.TRUE);
        }
        Optional<SimplePowerGrid> powerGrids = SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return List.of();
        SimplePowerGrid grid = powerGrids.get();
        final Optional<PowerComponentInfo> optional = grid.getInfoForPos(pos);
        if (optional.isEmpty()) return null;
        PowerComponentInfo componentInfo = optional.get();
        overloaded |= grid.getConsume() > grid.getGenerate();
        PowerComponentType type = componentInfo.type();

        if (overloaded) {
            for (int i = 1; i <= 3; i++) {
                lines.add(Component.translatable("tooltip.anvilcraft.grid_information.overloaded" + i));
            }
        }
        if (type == PowerComponentType.PRODUCER) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.producer_stats").withStyle(ChatFormatting.BLUE));
            lines.add(Component.translatable(
                "tooltip.anvilcraft.grid_information.output_power",
                componentInfo.produces()
            ).withStyle(ChatFormatting.GRAY));
        } else if (type == PowerComponentType.CONSUMER) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats").withStyle(ChatFormatting.BLUE));
            lines.add(Component.translatable(
                "tooltip.anvilcraft.grid_information.input_power",
                componentInfo.consumes()
            ).withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.translatable("tooltip.anvilcraft.grid_information.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.grid_information.total_consumed", grid.getConsume())
            .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.anvilcraft.grid_information.total_generated", grid.getGenerate())
            .withStyle(ChatFormatting.GRAY));

        double progress = charger.getProgress();
        lines.add(Component.translatable("tooltip.anvilcraft.working_progress.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable(
            "tooltip.anvilcraft.working_progress.progress",
            FormattingUtil.toShadeProgress(progress, 5), String.valueOf(((int) (progress * 10000)) / 100.0)
        ).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
