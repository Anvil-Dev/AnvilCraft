package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class PowerComponentTooltipProvider implements TooltipProvider {


    PowerComponentTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof IPowerComponent;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        boolean overloaded = false;
        IPowerComponent powerComponent;
        BlockPos pos;
        BlockState state;
        if (e instanceof IPowerComponent ipc) {
            if (e.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
                overloaded = e.getBlockState()
                        .getValues()
                        .getOrDefault(IPowerComponent.OVERLOAD, true)
                        .equals(Boolean.TRUE);
            }
            state = e.getBlockState();
            powerComponent = ipc;
            pos = e.getBlockPos();
        } else {
            return List.of();
        }
        List<SimplePowerGrid> powerGrids = SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return List.of();
        SimplePowerGrid grid = powerGrids.get(0);
        if (grid == null) return List.of();
        final PowerComponentInfo componentInfo = grid.getMappedPowerComponentInfo().get(pos);
        overloaded |= grid.getConsume() > grid.getGenerate();
        final List<Component> lines = new ArrayList<>();
        PowerComponentType type = powerComponent.getComponentType();

        if (overloaded) {
            for (int i = 1; i <= 3; i++) {
                lines.add(Component.translatable("tooltip.anvilcraft.grid_information.overloaded" + i));
            }
        }
        if (type == PowerComponentType.PRODUCER) {
            IPowerProducer producer = (IPowerProducer) powerComponent;
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.producer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.output_power",
                    componentInfo == null ? producer.getOutputPower() : componentInfo.produces()
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        } else if (type == PowerComponentType.CONSUMER) {
            IPowerConsumer consumer = (IPowerConsumer) powerComponent;
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.input_power",
                    componentInfo == null ? consumer.getInputPower() : componentInfo.consumes()
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }

        List<Component> tooltipLines = List.of(
                Component.translatable("tooltip.anvilcraft.grid_information.title")
                        .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)),
                Component.translatable(
                        "tooltip.anvilcraft.grid_information.total_consumed",
                        grid.getConsume()
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)),
                Component.translatable(
                        "tooltip.anvilcraft.grid_information.total_generated",
                        grid.getGenerate()
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
        );
        lines.addAll(tooltipLines);
        return lines;
    }

    @Override
    public ItemStack icon(BlockEntity entity) {
        return entity.getBlockState().getBlock().asItem().getDefaultInstance();
    }

    @Override
    public int priority() {
        return 0;
    }
}
