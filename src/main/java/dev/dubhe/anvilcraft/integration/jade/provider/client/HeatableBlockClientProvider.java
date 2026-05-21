package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.integration.jade.provider.HeatableBlockProvider;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HeatableBlockClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        HeatRecorder.getTier(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState()).ifPresent(
            tier -> tooltip.add(Component.translatable("tooltip.anvilcraft.heat.tier", tier.getDisplayName())));
        if (serverData.contains("duration")) {
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.heat.duration",
                FormattingUtil.toFormattedTime(serverData.getIntOr("duration", 0), 5)));
        }
    }

    @Override
    public Identifier getUid() {
        return HeatableBlockProvider.UID;
    }
}
