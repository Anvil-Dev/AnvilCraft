package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public enum PulseGeneratorClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof PulseGeneratorBlockEntity pulse)) {
            return;
        }
        Component mode = switch (pulse.getStartMode()) {
            case RISING_EDGE -> Component.translatable("tooltip.anvilcraft.pulse_generator.jade.mode.rising")
                .withStyle(ChatFormatting.GREEN);
            case FALLING_EDGE -> Component.translatable("tooltip.anvilcraft.pulse_generator.jade.mode.falling")
                .withStyle(ChatFormatting.RED);
            case LOOP -> Component.translatable("tooltip.anvilcraft.pulse_generator.jade.mode.loop")
                .withStyle(ChatFormatting.BLUE);
        };
        boolean reverse = pulse.isOutputInvert();
        Component reverseMode = Component.translatable("tooltip.anvilcraft.pulse_generator.jade.reverse." + (reverse ? "on" : "off"))
            .withStyle(reverse ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        tooltip.add(Component.translatable("tooltip.anvilcraft.pulse_generator.jade.mode_reverse", mode, reverseMode));

        boolean outputting = pulse.getState() == PulseGeneratorBlockEntity.State.OUTPUTTING;
        float elapsed = pulse.getPhaseProgress(0);
        Component times = Component.translatable("tooltip.anvilcraft.pulse_generator.delay",
            FormattingUtil.toFormattedTime(pulse.getWaitingTime(), 5))
            .append(" ").append(Component.translatable("tooltip.anvilcraft.pulse_generator.output_duration",
                FormattingUtil.toFormattedTime(pulse.getSignalDuration(), 5)));
        tooltip.add(JadeUI.progress(new ProgressView(
            ProgressView.Part.of(outputting ? 1 - elapsed : elapsed, outputting ? 0xFF8B0000 : 0xFF1E90FF),
            times, JadeUI.progressStyle(), BoxStyle.nestedBox()
        )));
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("pulse_generator");
    }
}
