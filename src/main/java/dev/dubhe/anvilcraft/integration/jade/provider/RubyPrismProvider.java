package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum RubyPrismProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("laser_level")) {
            int laserLevel = serverData.getInt("laser_level");
            iTooltip.add(Component.translatable("tooltip.anvilcraft.jade.ruby_prism.power", laserLevel));
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof RubyPrismBlockEntity blockEntity) {
            compoundTag.putInt("laser_level", blockEntity.laserLevel);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("ruby_prism");
    }
}
