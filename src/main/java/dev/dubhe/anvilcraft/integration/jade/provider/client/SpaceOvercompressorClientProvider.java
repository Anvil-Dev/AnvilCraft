package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.SpaceOvercompressorProvider;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SpaceOvercompressorClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig pluginConfig) {
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("storedMass")) {
            long mass = serverData.getLongOr("storedMass", 0L);
            tooltip.add(Component.translatable("tooltip.anvilcraft.space_overcompressor.stored_mass",
                MassInjectRecipe.displayMassValue(mass)));
        }
    }

    @Override
    public Identifier getUid() {
        return SpaceOvercompressorProvider.UID;
    }
}
