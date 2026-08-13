package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.LoadMonitorBlockEntity;
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

public enum LoadMonitorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    private static final BoxStyle.GradientBorder STYLE = BoxStyle.GradientBorder.TRANSPARENT.clone();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains("generate") || !serverData.contains("consume")) return;
        int generate = serverData.getInt("generate");
        int consume = serverData.getInt("consume");
        int percent = generate > 0 ? (int) Math.ceil(consume * 100.0 / generate) : 0;
        percent = Math.min(percent, 100);
        int color = percent < 75 ? 0xFFFFD700 : 0xFFFF0000;
        IElementHelper helper = IElementHelper.get();
        tooltip.add(helper.progress(
            percent / 100.0f,
            Component.translatable("tooltip.anvilcraft.load_monitor.jade.load", percent + "%"),
            helper.progressStyle().color(color).textColor(-1),
            Util.make(STYLE, boxStyle -> {
                boxStyle.borderColor = new int[]{0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0};
                boxStyle.borderWidth = 1.0f;
                boxStyle.bgColor = 0xFF32CD32;
            }),
            true
        ));
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
