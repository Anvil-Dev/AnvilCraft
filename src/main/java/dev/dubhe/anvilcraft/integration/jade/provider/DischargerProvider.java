package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
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

public enum DischargerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final BoxStyle.GradientBorder STYLE = BoxStyle.GradientBorder.TRANSPARENT.clone();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("discharger_timeLeft") || !data.contains("discharger_timeTotalCache")) return;

        int timeLeft = data.getInt("discharger_timeLeft");
        int timeTotalCache = data.getInt("discharger_timeTotalCache");
        boolean feDischarging = data.getBoolean("discharger_feDischarging");

        if (timeTotalCache <= 0) return;

        // 放电器：进度从满衰减到空 (remaining / total)
        double progress = Math.max(0, Math.min(1, (double) timeLeft / timeTotalCache));

        IElementHelper helper = IElementHelper.get();

        // 进度条 - 橙色表示放电
        tooltip.add(helper.progress(
            (float) progress,
            Component.translatable("tooltip.anvilcraft.discharger.jade.working_progress",
                Component.literal(String.format("%.1f%%", progress * 100))),
            helper.progressStyle().color(0xFFFF8C00).textColor(-1),
            Util.make(STYLE.clone(), box -> {
                box.borderColor = new int[]{0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0};
                box.borderWidth = 1.0f;
                box.bgColor = 0xFFFF8C00;
            }),
            true));

        // 时间或 FE 数值
        if (feDischarging) {
            // FE放电：显示剩余电量 / 总容量
            tooltip.add(Component.translatable("tooltip.anvilcraft.discharger.jade.energy",
                UnitUtil.energyUnit(timeLeft, false),
                UnitUtil.energyUnit(timeTotalCache, false)));
        } else {
            tooltip.add(Component.translatable("tooltip.anvilcraft.discharger.jade.time",
                FormattingUtil.toFormattedTime(timeLeft),
                FormattingUtil.toFormattedTime(timeTotalCache)));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof DischargerBlockEntity discharger) {
            tag.putInt("discharger_timeLeft", discharger.getTimeLeft());
            tag.putInt("discharger_timeTotalCache", discharger.getTimeTotalCache());
            tag.putBoolean("discharger_feDischarging", discharger.isFeDischarging());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("discharger_provider");
    }
}
