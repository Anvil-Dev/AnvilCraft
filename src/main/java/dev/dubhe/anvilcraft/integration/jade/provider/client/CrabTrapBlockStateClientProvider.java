package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.CrabTrapBlockStateProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CrabTrapBlockStateClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig pluginConfig) {
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("finishing")) {
            int finishing = serverData.getIntOr("finishing", 0);
            tooltip.add(Component.translatable("tooltip.anvilcraft.crab_trap.jade.finishing", finishing));
        }
    }

    @Override
    public Identifier getUid() {
        return CrabTrapBlockStateProvider.UID;
    }
}
