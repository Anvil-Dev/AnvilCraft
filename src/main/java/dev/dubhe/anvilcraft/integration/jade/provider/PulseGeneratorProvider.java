package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum PulseGeneratorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final BoxStyle.GradientBorder STYLE = BoxStyle.GradientBorder.TRANSPARENT.clone();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("pulse_generator_mode")) return;
        String mode = data.getString("pulse_generator_mode");
        boolean reverse = data.getBoolean("pulse_generator_reverse");
        ChatFormatting modeColor = switch (mode) {
            case "rising" -> ChatFormatting.GREEN;
            case "falling" -> ChatFormatting.RED;
            case "loop" -> ChatFormatting.BLUE;
            default -> ChatFormatting.GRAY;
        };
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.pulse_generator.jade.mode_reverse",
            Component.translatable("tooltip.anvilcraft.pulse_generator.jade.mode." + mode)
                .withStyle(modeColor),
            Component.translatable("tooltip.anvilcraft.pulse_generator.jade.reverse." + (reverse ? "on" : "off"))
                .withStyle(reverse ? ChatFormatting.GOLD : ChatFormatting.GRAY)
        ));

        int remaining = data.getInt("pulse_generator_remaining_ticks");
        int total = data.getInt("pulse_generator_total_ticks");
        if (total > 0) {
            // 工作中显示当前相位进度；空闲时按配置的延迟时长显示完整进度条，保证进度条始终可见
            boolean processing = data.getBoolean("pulse_generator_processing");
            boolean outputting = "OUTPUTTING".equals(data.getString("pulse_generator_state"));
            double progress;
            if (processing) {
                progress = outputting
                           ? (double) remaining / total
                           : 1 - (double) remaining / total;
            } else {
                progress = outputting ? 1.0 : 0.0;
            }
            progress = Math.max(0, Math.min(1, progress));
            int barColor = outputting ? 0xFF8B0000 : 0xFF1E90FF;
            IElementHelper helper = IElementHelper.get();
            tooltip.add(helper.progress(
                (float) progress,
                Component.translatable("tooltip.anvilcraft.pulse_generator.delay",
                    Component.literal(FormattingUtil.toFormattedTime(data.getInt("pulse_generator_delay"), 5))
                ).append(" ").append(Component.translatable("tooltip.anvilcraft.pulse_generator.output_duration",
                    Component.literal(FormattingUtil.toFormattedTime(data.getInt("pulse_generator_duration"), 5)))),
                helper.progressStyle().color(barColor).textColor(0xFFE0E0E0),
                Util.make(STYLE.clone(), box -> {
                    box.borderColor = new int[]{0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0};
                    box.borderWidth = 1.0f;
                    box.bgColor = 0xFF202020;
                }),
                true));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof PulseGeneratorBlockEntity pulseGenerator)) return;
        String mode = switch (pulseGenerator.getStartMode()) {
            case RISING_EDGE -> "rising";
            case FALLING_EDGE -> "falling";
            case LOOP -> "loop";
        };
        tag.putString("pulse_generator_mode", mode);
        tag.putBoolean("pulse_generator_reverse", pulseGenerator.isOutputInvert());
        tag.putInt("pulse_generator_delay", pulseGenerator.getWaitingTime());
        tag.putInt("pulse_generator_duration", pulseGenerator.getSignalDuration());
        boolean processing = pulseGenerator.isProcessing();
        tag.putBoolean("pulse_generator_processing", processing);
        tag.putString("pulse_generator_state", pulseGenerator.getState().name());
        // 空闲时也写入相位时长，使 Jade 进度条始终有值可渲染
        tag.putInt(
            "pulse_generator_remaining_ticks",
            processing ? pulseGenerator.getPhaseRemainingTicks() : 0);
        tag.putInt(
            "pulse_generator_total_ticks",
            processing
                ? pulseGenerator.getPhaseDuration()
                : Math.max(pulseGenerator.getWaitingTime(), 1));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("pulse_generator");
    }
}
