package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DischargerTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof DischargerBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
        if (!(value instanceof DischargerBlockEntity discharger)) return List.of();

        final List<Component> lines = new ArrayList<>();
        BlockPos pos = discharger.getBlockPos();
        boolean overloaded = false;
        if (value.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
            overloaded = value.getBlockState().getValues().getOrDefault(IPowerComponent.OVERLOAD, true).equals(Boolean.TRUE);
        }
        Optional<SimplePowerGrid> powerGrids = SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return List.of();
        SimplePowerGrid grid = powerGrids.get();
        final Optional<PowerComponentInfo> optional = grid.getInfoForPos(pos);
        if (optional.isEmpty()) return List.of();
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
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.output_power",
                    UnitUtil.electricityUnit(componentInfo.produces(), false, componentInfo.infinitePower()))
                .withStyle(ChatFormatting.GRAY));
        } else if (type == PowerComponentType.CONSUMER) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats").withStyle(ChatFormatting.BLUE));
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.input_power",
                    UnitUtil.electricityUnit(componentInfo.consumes(), false, false))
                .withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.translatable("tooltip.anvilcraft.grid_information.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.grid_information.total_consumed",
                UnitUtil.electricityUnit(grid.getConsume(), false, false))
            .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.anvilcraft.grid_information.total_generated",
                UnitUtil.electricityUnit(grid.getGenerate(), false, grid.isInfinitePower()))
            .withStyle(ChatFormatting.GRAY));

        // 放电器进度：从满衰减到空 (remaining / total)
        int timeLeft = discharger.getTimeLeft();
        int timeTotalCache = discharger.getTimeTotalCache();
        double progress = discharger.getProgress(); // timeLeft / timeTotalCache in DischargerBlockEntity

        lines.add(Component.translatable("tooltip.anvilcraft.working_progress.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable(
            "tooltip.anvilcraft.working_progress.progress",
            FormattingUtil.toShadeProgress(progress, 5),
            String.valueOf(((int) (progress * 10000)) / 100.0)
        ).withStyle(ChatFormatting.GRAY));
        if (discharger.isFeDischarging()) {
            // FE放电：显示剩余电量 / 总容量
            MutableComponent feLine = Component.literal("  ").withStyle(ChatFormatting.GRAY)
                .append(UnitUtil.energyUnit(timeLeft, false))
                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(UnitUtil.energyUnit(timeTotalCache, false));
            lines.add(feLine);
        } else if (timeTotalCache > 0) {
            lines.add(Component.translatable(
                "tooltip.anvilcraft.working_progress.time",
                FormattingUtil.toFormattedTime(timeLeft),
                FormattingUtil.toFormattedTime(timeTotalCache)
            ).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
