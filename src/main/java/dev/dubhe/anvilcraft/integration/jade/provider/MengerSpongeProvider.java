package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/**
 * 门格海绵的 Jade 提示：流体容量显示为无限（只进不出的虚空容器），
 * 而非默认提供器读取容量上限时显示的 2.14B。
 */
public class MengerSpongeProvider extends FluidStorageProvider.ForBlock {
    public static final MengerSpongeProvider INSTANCE = new MengerSpongeProvider();

    private MengerSpongeProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        IElementHelper helper = IElementHelper.get();
        tooltip.clear();
        tooltip.add(Component.translatable("block.anvilcraft.menger_sponge").withStyle(ChatFormatting.WHITE));
        tooltip.add(helper.progress(1.0f,
            Component.translatable("jade.fluid.empty").withStyle(ChatFormatting.WHITE)
                .append(" ")
                .append(Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY)),
            helper.progressStyle().color(0x00000000, 0x00000000),
            BoxStyle.getNestedBox(), true));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("menger_sponge");
    }
}
