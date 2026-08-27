package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.LoadMonitorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LoadMonitorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains("generate") || !serverData.contains("consume")) return;
        int generate = serverData.getInt("generate");
        int consume = serverData.getInt("consume");
        int percent = generate > 0 ? (int) Math.ceil(consume * 100.0 / generate) : 0;
        percent = Math.min(percent, 100);
        int color = percent < 75 ? 0xFFFFD700 : 0xFFFF0000;
        tooltip.add(Component.translatable("tooltip.anvilcraft.load_monitor.jade.load", percent + "%").withColor(color));
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof LoadMonitorBlockEntity entity) {
            PowerGrid grid = entity.getGrid();
            if (grid == null) return;
            tag.putInt("generate", grid.getGenerate());
            tag.putInt("consume", grid.getConsume());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("load_monitor");
    }
}
