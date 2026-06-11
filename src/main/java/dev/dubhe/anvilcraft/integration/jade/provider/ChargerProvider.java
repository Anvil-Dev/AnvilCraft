package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
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

public enum ChargerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final BoxStyle.GradientBorder STYLE = BoxStyle.GradientBorder.TRANSPARENT.clone();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("charger_timeLeft") || !data.contains("charger_timeTotalCache")) return;

        int timeLeft = data.getInt("charger_timeLeft");
        int timeTotalCache = data.getInt("charger_timeTotalCache");
        boolean feCharging = data.getBoolean("charger_feCharging");

        if (timeTotalCache <= 0) return;

        double progress = Math.max(0, Math.min(1, 1 - (double) timeLeft / timeTotalCache));

        IElementHelper helper = IElementHelper.get();

        // 进度条
        tooltip.add(helper.progress(
            (float) progress,
            Component.translatable("tooltip.anvilcraft.charger.jade.working_progress",
                Component.literal(String.format("%.1f%%", progress * 100))),
            helper.progressStyle().color(0xFF87CEEB).textColor(-1),
            Util.make(STYLE.clone(), box -> {
                box.borderColor = new int[]{0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0};
                box.borderWidth = 1.0f;
                box.bgColor = 0xFF32CD32;
            }),
            true));

        // 时间或 FE 数值
        if (feCharging) {
            int currentEnergy = timeTotalCache - timeLeft;
            tooltip.add(Component.translatable("tooltip.anvilcraft.charger.jade.energy",
                UnitUtil.energyUnit(currentEnergy, false),
                UnitUtil.energyUnit(timeTotalCache, false)));
        } else {
            tooltip.add(Component.translatable("tooltip.anvilcraft.charger.jade.time",
                FormattingUtil.toFormattedTime(timeLeft),
                FormattingUtil.toFormattedTime(timeTotalCache)));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof ChargerBlockEntity charger) {
            tag.putInt("charger_timeLeft", charger.getTimeLeft());
            tag.putInt("charger_timeTotalCache", charger.getTimeTotalCache());
            tag.putBoolean("charger_feCharging", charger.isFeCharging());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("charger_provider");
    }
}
