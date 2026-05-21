package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.RubyPrismProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum RubyPrismClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig pluginConfig) {
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("laser_level")) {
            int laserLevel = serverData.getIntOr("laser_level", 0);
            tooltip.add(Component.translatable("tooltip.anvilcraft.jade.ruby_prism.power", laserLevel));
        }
    }

    @Override
    public Identifier getUid() {
        return RubyPrismProvider.UID;
    }
}
