package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.CauldronProcessingProvider;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** 在 Jade 的物品和流体信息旁显示当前容器的加工预览。 */
public enum CauldronProcessingClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CauldronProcessingProvider.readLines(accessor).forEach(tooltip::add);
    }

    @Override
    public Identifier getUid() {
        return CauldronProcessingProvider.UID;
    }
}
