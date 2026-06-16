package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public enum DischargerClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("discharger_timeLeft") || !data.contains("discharger_timeTotalCache")) return;

        int timeLeft = data.getIntOr("discharger_timeLeft", 0);
        int timeTotalCache = data.getIntOr("discharger_timeTotalCache", 0);
        boolean feDischarging = data.getBooleanOr("discharger_feDischarging", false);

        if (timeTotalCache <= 0) return;

        double progress = Math.max(0, Math.min(1, (double) timeLeft / timeTotalCache));

        tooltip.add(JadeUI.progress(
            new ProgressView(
                ProgressView.Part.of((float) progress),
                Component.translatable("tooltip.anvilcraft.discharger.jade.working_progress",
                    Component.literal(String.format("%.1f%%", progress * 100))),
                JadeUI.progressStyle(),
                BoxStyle.nestedBox()
            )));

        if (feDischarging) {
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
    public Identifier getUid() {
        return AnvilCraft.of("discharger_client_provider");
    }
}
