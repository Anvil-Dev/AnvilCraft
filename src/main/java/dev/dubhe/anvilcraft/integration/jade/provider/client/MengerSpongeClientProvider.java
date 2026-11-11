package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

/**
 * 门格海绵的 Jade 提示：流体容量显示为无限（只进不出的虚空容器），
 * 而非默认流体提供器读取容量上限时显示的 2.14B。纯客户端，无需服务端数据。
 */
public enum MengerSpongeClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // 通用流体提供器会先按 VoidFluidHandler 的 Integer.MAX_VALUE 容量画一条 21 亿的进度条；
        // 本组件在它之后执行，清掉整条提示后只保留方块名与唯一的无限条。
        tooltip.clear();
        tooltip.add(accessor.getBlock().getName().copy().withStyle(ChatFormatting.WHITE));
        tooltip.add(JadeUI.progress(
            new ProgressView(
                // 透明填充：只保留“Empty ∞”文字，不绘制白色进度条底色
                ProgressView.Part.of(1.0f, 0x00000000),
                Component.translatable("jade.fluid.empty").withStyle(ChatFormatting.GRAY)
                    .append(" ")
                    .append(Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY)),
                JadeUI.progressStyle(),
                BoxStyle.nestedBox()
            )));
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("menger_sponge_client_provider");
    }
}
