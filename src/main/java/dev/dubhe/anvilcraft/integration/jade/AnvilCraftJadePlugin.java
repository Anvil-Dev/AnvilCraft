package dev.dubhe.anvilcraft.integration.jade;

import dev.dubhe.anvilcraft.block.WipBlock;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.production.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.utility.MengerSpongeBlock;
import dev.dubhe.anvilcraft.integration.jade.provider.BurningHeaterProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.ChargerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CrabTrapBlockStateProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CreativeCrateProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CreativeFluidTankProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.DischargerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.HeatableBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.ItemDetectorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.LargeFluidTankProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.MultiPartPowerBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.PowerBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.RubyPrismProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.SmartBlockPlacerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.SpaceOvercompressorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.WipBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.BurningHeaterClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.ChargerClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.CrabTrapBlockStateClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.CreativeCrateClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.CreativeFluidTankClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.DischargerClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.HeatableBlockClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.ItemDetectorClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.LargeFluidTankClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.MengerSpongeClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.PowerBlockClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.RubyPrismClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.SmartBlockPlacerClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.SpaceOvercompressorClientProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.client.WipBlockClientProvider;
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
        registration.registerBlockDataProvider(PowerBlockProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(RubyPrismProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(ItemDetectorProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(SpaceOvercompressorProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(CrabTrapBlockStateProvider.INSTANCE, CrabTrapBlock.class);
        registration.registerBlockDataProvider(HeatableBlockProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(BurningHeaterProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(ChargerProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(DischargerProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(SmartBlockPlacerProvider.INSTANCE, Block.class);
        registration.registerItemStorage(CreativeCrateProvider.INSTANCE, CreativeCrateBlockEntity.class);
        registration.registerFluidStorage(CreativeFluidTankProvider.INSTANCE, CreativeFluidTankBlockEntity.class);
        registration.registerFluidStorage(LargeFluidTankProvider.INSTANCE, LargeFluidTankBlock.class);
        registration.registerBlockDataProvider(WipBlockProvider.INSTANCE, WipBlock.class);
        registration.registerBlockDataProvider(MultiPartPowerBlockProvider.INSTANCE, AbstractMultiPartBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PowerBlockClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(BurningHeaterClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(RubyPrismClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(ItemDetectorClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(SpaceOvercompressorClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(CrabTrapBlockStateClientProvider.INSTANCE, CrabTrapBlock.class);
        registration.registerBlockComponent(HeatableBlockClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(ChargerClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(DischargerClientProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(SmartBlockPlacerClientProvider.INSTANCE, Block.class);
        registration.registerItemStorageClient(CreativeCrateClientProvider.INSTANCE);
        registration.registerFluidStorageClient(CreativeFluidTankClientProvider.INSTANCE);
        registration.registerFluidStorageClient(LargeFluidTankClientProvider.INSTANCE);
        registration.registerBlockComponent(MengerSpongeClientProvider.INSTANCE, MengerSpongeBlock.class);
        registration.registerBlockComponent(WipBlockClientProvider.INSTANCE, WipBlock.class);
    }
}
