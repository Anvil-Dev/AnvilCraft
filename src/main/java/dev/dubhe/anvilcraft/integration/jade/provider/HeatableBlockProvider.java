package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HeatableBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        HeatRecorder.getTier(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState()).ifPresent(
            tier -> tooltip.add(Component.translatable("tooltip.anvilcraft.heat.tier", tier.toComponent())));
        if (serverData.contains("duration")) {
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.heat.duration",
                FormattingUtil.toFormattedTime(serverData.getInt("duration"), 5)));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof HeatableBlockEntity entity) {
            tag.putInt("duration", entity.getDuration());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("heatable_block_provider");
    }
}
