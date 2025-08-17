package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.util.UnitUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PowerComponentTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public PowerComponentTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof IPowerComponent;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        boolean original = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isShiftKeyDown()) {
            original = true;
        }
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) return null;
        boolean overloaded = false;
        BlockPos pos;
        BlockState blockState = e.getBlockState();
        if (blockState.getBlock() instanceof AbstractMultiPartBlock<?> multiPartBlock) {
            pos = multiPartBlock.getMainPartPos(e.getBlockPos(), e.getBlockState());
            if (blockState.getBlock() instanceof TransmissionPoleBlock) {
                pos = pos.above(2);
            } else if (blockState.getBlock() instanceof RemoteTransmissionPoleBlock) {
                pos = pos.above(3);
            }
        } else if (e instanceof IPowerComponent) {
            if (e.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
                overloaded = e.getBlockState()
                    .getValues()
                    .getOrDefault(IPowerComponent.OVERLOAD, true)
                    .equals(Boolean.TRUE);
            }
            pos = e.getBlockPos();
        } else {
            return List.of();
        }
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
                UnitUtil.electricityUnit(grid.getConsume(), original)
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)),
            Component.translatable("tooltip.anvilcraft.grid_information.total_generated",
                UnitUtil.electricityUnit(grid.getGenerate(), original)
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        lines.addAll(tooltipLines);
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
