package dev.dubhe.anvilcraft.integration.jade;

import dev.dubhe.anvilcraft.block.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.CreativeCrateBlock;
import dev.dubhe.anvilcraft.block.CreativeFluidTankBlock;
import dev.dubhe.anvilcraft.block.CursedGoldBlock;
import dev.dubhe.anvilcraft.block.ExpCollectorBlock;
import dev.dubhe.anvilcraft.block.FluidTankBlock;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.block.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.LoadMonitorBlock;
import dev.dubhe.anvilcraft.block.MengerSpongeBlock;
import dev.dubhe.anvilcraft.block.PulseGeneratorBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.integration.jade.provider.AutoEnchantingTableProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.BurningHeaterProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.ChargerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CollectorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.ControlValveProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CrabTrapBlockStateProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CreativeCrateProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CreativeFluidTankProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.CursedGoldEnchantPowerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.DischargerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.FluidTankProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.HeatableBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.ItemDetectorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.LoadMonitorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.MengerSpongeProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.MultiPartPowerBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.PowerBlockProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.PulseGeneratorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.RedstoneWireProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.RubyPrismProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.SmartBlockPlacerProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.SpaceOvercompressorProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.WipBlockProvider;
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
        registration.registerBlockDataProvider(ItemDetectorProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(SpaceOvercompressorProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(HeatableBlockProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(BurningHeaterProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(SmartBlockPlacerProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(ChargerProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(DischargerProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(AutoEnchantingTableProvider.INSTANCE, AutoEnchantingTableBlockEntity.class);
        registration.registerBlockDataProvider(PulseGeneratorProvider.INSTANCE, PulseGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(WipBlockProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(CreativeFluidTankProvider.INSTANCE, CreativeFluidTankBlockEntity.class);
        registration.registerBlockDataProvider(LoadMonitorProvider.INSTANCE, LoadMonitorBlock.class);
        registration.registerBlockDataProvider(FluidTankProvider.INSTANCE, FluidTankBlockEntity.class);
        registration.registerBlockDataProvider(FluidTankProvider.INSTANCE, LargeFluidTankBlockEntity.class);
        registration.registerBlockDataProvider(CreativeCrateProvider.INSTANCE, CreativeCrateBlockEntity.class);
        registration.registerBlockDataProvider(MengerSpongeProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(CrabTrapBlockStateProvider.INSTANCE, CrabTrapBlock.class);
        registration.registerBlockDataProvider(MultiPartPowerBlockProvider.INSTANCE, AbstractMultiPartBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PowerBlockProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(RubyPrismProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(ItemDetectorProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(SpaceOvercompressorProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(HeatableBlockProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(BurningHeaterProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(SmartBlockPlacerProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(ChargerProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(DischargerProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(AutoEnchantingTableProvider.INSTANCE, AutoEnchantingTableBlock.class);
        registration.registerBlockComponent(PulseGeneratorProvider.INSTANCE, PulseGeneratorBlock.class);
        registration.registerBlockComponent(WipBlockProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(CreativeFluidTankProvider.INSTANCE, CreativeFluidTankBlock.class);
        registration.registerBlockComponent(ControlValveProvider.INSTANCE, ControlValveBlock.class);
        registration.registerBlockComponent(CollectorProvider.INSTANCE, ItemCollectorBlock.class);
        registration.registerBlockComponent(CollectorProvider.INSTANCE, ExpCollectorBlock.class);
        registration.registerBlockComponent(LoadMonitorProvider.INSTANCE, LoadMonitorBlock.class);
        registration.registerBlockComponent(RedstoneWireProvider.INSTANCE, RedstoneWireBlock.class);
        registration.registerBlockComponent(FluidTankProvider.INSTANCE, FluidTankBlock.class);
        registration.registerBlockComponent(FluidTankProvider.INSTANCE, LargeFluidTankBlock.class);
        registration.registerBlockComponent(CreativeCrateProvider.INSTANCE, CreativeCrateBlock.class);
        registration.registerBlockComponent(MengerSpongeProvider.INSTANCE, MengerSpongeBlock.class);
        registration.registerBlockComponent(CrabTrapBlockStateProvider.INSTANCE, CrabTrapBlock.class);
        registration.registerBlockComponent(CursedGoldEnchantPowerProvider.INSTANCE, CursedGoldBlock.class);
    }
}
