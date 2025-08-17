package dev.dubhe.anvilcraft.integration.jade;

import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.integration.jade.provider.CrabTrapStorageProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.HeatableBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.ItemDetectorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.PowerBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.RubyPrismProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.SpaceOvercompressorProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@SuppressWarnings("unused")
@WailaPlugin
public class AnvilCraftJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PowerBlockProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(RubyPrismProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(ItemDetectorProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(SpaceOvercompressorProvider.INSTANCE, BlockEntity.class);
        registration.registerItemStorage(CrabTrapStorageProvider.INSTANCE, CrabTrapBlockEntity.class);
        registration.registerBlockDataProvider(HeatableBlockProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PowerBlockProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(RubyPrismProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(ItemDetectorProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(SpaceOvercompressorProvider.INSTANCE, Block.class);
        registration.registerItemStorageClient(CrabTrapStorageProvider.INSTANCE);
        registration.registerBlockComponent(HeatableBlockProvider.INSTANCE, Block.class);
    }
}
