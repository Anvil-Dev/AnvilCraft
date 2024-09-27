package dev.dubhe.anvilcraft.integration.jade;

import dev.dubhe.anvilcraft.integration.jade.provider.PowerBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.RubyPrismProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class AnvilCraftJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PowerBlockProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(RubyPrismProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PowerBlockProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(RubyPrismProvider.INSTANCE, Block.class);
    }
}
