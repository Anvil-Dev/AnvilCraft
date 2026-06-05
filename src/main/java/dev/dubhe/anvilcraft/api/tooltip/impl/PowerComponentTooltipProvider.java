package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public List<Component> tooltip(BlockEntity e) {
        boolean original = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isShiftKeyDown()) {
            original = true;
        }
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
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
                overloaded = e.getBlockState().getValues().getOrDefault(IPowerComponent.OVERLOAD, true).equals(Boolean.TRUE);
            }
            pos = e.getBlockPos();
        } else {
            return List.of();
        }
        Optional<SimplePowerGrid> powerGrids = SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return List.of();
        SimplePowerGrid grid = powerGrids.get();
        final Optional<PowerComponentInfo> optional = grid.getInfoForPos(pos);
        if (optional.isEmpty()) return List.of();
        PowerComponentInfo componentInfo = optional.get();
        overloaded |= grid.getConsume() > grid.getGenerate();
        final List<Component> lines = new ArrayList<>();

        if (overloaded) {
            for (int i = 1; i <= 3; i++) {
                lines.add(Component.translatable("tooltip.anvilcraft.grid_information.overloaded" + i));
            }
        }
        if (e instanceof FeCollectorBlockEntity fe) {
            lines.add(Component.translatable("tooltip.anvilcraft.fe_collector.title")
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.fe_collector.energy",
                    fe.getEnergyStored() / 1000,
                    FeCollectorBlockEntity.MAX_ENERGY / 1000
                )
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            if (!e.getBlockState().getValue(BlockStateProperties.POWERED)) {
                lines.add(Component.translatable("tooltip.anvilcraft.fe_collector.low_energy")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED)));
            }
        }
        if (e instanceof PowerConverterBlockEntity be) {
            lines.add(Component.translatable("tooltip.anvilcraft.power_converter.fe_stored")
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.power_converter.fe_stored.value",
                    be.getEnergyStored() / 1000,
                    be.getMaxEnergyStored() / 1000
                )
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }
        PowerComponentType type = componentInfo.type();
        if (type == PowerComponentType.PRODUCER) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.producer_stats")
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.output_power",
                    UnitUtil.electricityUnit(componentInfo.produces(), original)
                )
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        } else if (type == PowerComponentType.CONSUMER) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats")
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            int consumes = componentInfo.consumes();
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.input_power",
                    UnitUtil.electricityUnit(consumes, original)
                )
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }

        List<Component> tooltipLines = List.of(
            Component.translatable("tooltip.anvilcraft.grid_information.title").setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)),
            Component.translatable(
                    "tooltip.anvilcraft.grid_information.total_consumed",
                    UnitUtil.electricityUnit(grid.getConsume(), original)
                )
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)),
            Component.translatable(
                    "tooltip.anvilcraft.grid_information.total_generated",
                    UnitUtil.electricityUnit(grid.getGenerate(), original)
                )
                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
        );
        lines.addAll(tooltipLines);
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
