package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.integration.jade.provider.PowerBlockProvider;
import dev.dubhe.anvilcraft.util.ColorUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public enum PowerBlockClientProvider implements IBlockComponentProvider {
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


            float percent = Mth.clamp(MathUtil.safeDiv(consume, generate), 0, 1);
            int color = ColorUtil.colorFromRatio(percent, false);

            tooltip.add(JadeUI.progress(
                new ProgressView(
                ProgressView.Part.of(
                    percent,
                    JadeUI.horizontalTiledSprite(RenderPipelines.GUI_TEXTURED, JadeIds.JADE("energy_progress"), 16, 16)
                ),
                Component.translatable(
                    "tooltip.anvilcraft.jade.power_information",
                    UnitUtil.electricityUnit(consume, generate, original)
                ).withColor(color),
                JadeUI.progressStyle(),
                BoxStyle.nestedBox()
            )));
        }
    }

    @Override
    public Identifier getUid() {
        return PowerBlockProvider.UID;
    }
}
