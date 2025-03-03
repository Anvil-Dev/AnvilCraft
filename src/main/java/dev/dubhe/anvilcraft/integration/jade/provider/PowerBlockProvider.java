package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;

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

public enum PowerBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    private static final BoxStyle.GradientBorder STYLE = BoxStyle.GradientBorder.TRANSPARENT.clone();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (serverData.contains("generate") && serverData.contains("consume")) {
            IElementHelper elementHelper = IElementHelper.get();
            int generate = serverData.getInt("generate");
            int consume = serverData.getInt("consume");

            int color;
            float percent = (float) consume / generate;
            if (percent < 0.75) {
                color = 0xFFFFD700;
            } else {
                color = 0xFFFF0000;
            }

            tooltip.add(elementHelper.progress(
                percent,
                Component.translatable("tooltip.anvilcraft.jade.power_information", consume, generate),
                elementHelper.progressStyle().color(color).textColor(-1),
                Util.make(STYLE, boxStyle -> {
                    boxStyle.borderColor = new int[]{0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0};
                    boxStyle.borderWidth = 1.0f;
                    boxStyle.bgColor = 0xFF32CD32;
                }),
                true));
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IPowerComponent blockEntity) {
            PowerGrid powerGrid = blockEntity.getGrid();
            if (powerGrid == null) {
                return;
            }
            compoundTag.putInt("generate", powerGrid.getGenerate());
            compoundTag.putInt("consume", powerGrid.getConsume());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("power_provider");
    }
}
