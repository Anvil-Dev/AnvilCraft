package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.util.UnitUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HeatCollectorTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public HeatCollectorTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof HeatCollectorBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        boolean original = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isShiftKeyDown()) {
            original = true;
        }
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) return null;
        if (!(e instanceof HeatCollectorBlockEntity heatCollector)) return null;
        if (!heatCollector.isWorking()) {
            return List.of(
                Component.translatable("tooltip.anvilcraft.heat_collector.not_work").withStyle(ChatFormatting.RED),
                Component.translatable(heatCollector.getResult().getTranslateKey())
            );
        }
        boolean overloaded = false;
        BlockPos pos;
        if (e.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
            overloaded = e.getBlockState()
                .getValues()
                .getOrDefault(IPowerComponent.OVERLOAD, true)
                .equals(Boolean.TRUE);
        }
        pos = e.getBlockPos();
        Optional<SimplePowerGrid> powerGrids = SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return List.of();
        SimplePowerGrid grid = powerGrids.get();
        final Optional<PowerComponentInfo> optional = grid.getInfoForPos(pos);
        if (optional.isEmpty()) return null;
        PowerComponentInfo componentInfo = optional.get();
        overloaded |= grid.getConsume() > grid.getGenerate();
        final List<Component> lines = new ArrayList<>();
        PowerComponentType type = componentInfo.type();

        if (overloaded) {
            for (int i = 1; i <= 3; i++) {
                lines.add(Component.translatable("tooltip.anvilcraft.grid_information.overloaded" + i));
            }
        }
        if (type == PowerComponentType.PRODUCER) {
            lines.add(
                Component.translatable("tooltip.anvilcraft.grid_information.producer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(
                Component.translatable(
                    "tooltip.anvilcraft.grid_information.output_power",
                    UnitUtil.electricityUnit(componentInfo.produces(), original)
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
            );
        } else if (type == PowerComponentType.CONSUMER) {
            lines.add(
                Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(
                Component.translatable(
                    "tooltip.anvilcraft.grid_information.input_power",
                    UnitUtil.electricityUnit(componentInfo.consumes(), original)
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }

        List<Component> tooltipLines = List.of(
            Component.translatable("tooltip.anvilcraft.grid_information.title")
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)),
            Component.translatable("tooltip.anvilcraft.grid_information.total_consumed",
                    UnitUtil.electricityUnit(grid.getConsume(), original))
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)),
            Component.translatable("tooltip.anvilcraft.grid_information.total_generated",
                    UnitUtil.electricityUnit(grid.getGenerate(), original))
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        lines.addAll(tooltipLines);
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
