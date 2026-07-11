package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class PulseGeneratorTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof PulseGeneratorBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity entity) {
        if (!(entity instanceof PulseGeneratorBlockEntity pulseGenerator)) return List.of();
        String modeKey = switch (pulseGenerator.getStartMode()) {
            case RISING_EDGE -> "screen.anvilcraft.button.pulse_generator.start_mode.rising";
            case FALLING_EDGE -> "screen.anvilcraft.button.pulse_generator.start_mode.falling";
            case LOOP -> "screen.anvilcraft.button.pulse_generator.start_mode.loop";
        };
        String reverseKey = pulseGenerator.isOutputInvert()
            ? "screen.anvilcraft.button.pulse_generator.reverse.on"
            : "screen.anvilcraft.button.pulse_generator.reverse.off";
        return List.of(
            Component.translatable("tooltip.anvilcraft.pulse_generator.title").withStyle(ChatFormatting.BLUE),
            ITooltipProvider.withIndentAndMerge(Component.translatable(modeKey).withStyle(ChatFormatting.GRAY)),
            ITooltipProvider.withIndentAndMerge(Component.translatable(reverseKey).withStyle(ChatFormatting.GRAY)),
            ITooltipProvider.withIndentAndMerge(Component.translatable(
                "tooltip.anvilcraft.pulse_generator.delay",
                FormattingUtil.toFormattedTime(pulseGenerator.getWaitingTime(), 5)
            ).withStyle(ChatFormatting.GRAY)),
            ITooltipProvider.withIndentAndMerge(Component.translatable(
                "tooltip.anvilcraft.pulse_generator.output_duration",
                FormattingUtil.toFormattedTime(pulseGenerator.getSignalDuration(), 5)
            ).withStyle(ChatFormatting.GRAY))
        );
    }

    @Override
    public int priority() {
        return 0;
    }
}
