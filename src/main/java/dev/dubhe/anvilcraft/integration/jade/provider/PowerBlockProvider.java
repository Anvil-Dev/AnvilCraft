package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.ColorPalette;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public enum PowerBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    private static final BoxStyle STYLE = BoxStyle.transparent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean original = player != null && player.isShiftKeyDown();
        CompoundTag serverData = accessor.getServerData();
        if (serverData.contains("generate") && serverData.contains("consume")) {
            int generate = serverData.getIntOr("generate", 0);
            int consume = serverData.getIntOr("consume", 0);

            int color;
            float percent = (float) consume / generate;
            if (percent < 0.75) {
                color = 0xFFFFD700;
            } else {
                color = 0xFFFF0000;
            }

            tooltip.add(JadeUI.progress(new ProgressView(
                ProgressView.Part.of(percent, JadeUI.sprite(JadeIds.UNIVERSAL_PROGRESS, 1, 1)),
                Component.translatable("tooltip.anvilcraft.jade.power_information", UnitUtil.electricityUnit(consume, generate, original)),
                JadeUI.progressStyle().fitContentX(false).fitContentY(false),
                Util.make(STYLE, boxStyle -> {
                    boxStyle.boxProgressColors = new ColorPalette(0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0);
                    boxStyle.borderWidth = 1;
                })
            )));
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
    public Identifier getUid() {
        return AnvilCraft.of("power_provider");
    }
}
