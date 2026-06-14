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

public enum ChargerClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("charger_timeLeft") || !data.contains("charger_timeTotalCache")) return;

        int timeLeft = data.getIntOr("charger_timeLeft", 0);
        int timeTotalCache = data.getIntOr("charger_timeTotalCache", 0);
        boolean feCharging = data.getBooleanOr("charger_feCharging", false);

        if (timeTotalCache <= 0) return;

        double progress = Math.max(0, Math.min(1, 1 - (double) timeLeft / timeTotalCache));

        tooltip.add(JadeUI.progress(
            new ProgressView(
                ProgressView.Part.of((float) progress),
                Component.translatable("tooltip.anvilcraft.charger.jade.working_progress",
                    Component.literal(String.format("%.1f%%", progress * 100))),
                JadeUI.progressStyle(),
                BoxStyle.transparent()
            )));

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
    public Identifier getUid() {
        return AnvilCraft.of("charger_client_provider");
    }
}
