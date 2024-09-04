package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.registrator.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LoadMonitorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MobAmberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RemoteTransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ResentfulAmberBlock;
import dev.dubhe.anvilcraft.block.entity.RubyLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ThermoelectricConverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BatchCrafterRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HasMobBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeliostatsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LaserRenderer;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATOR;

public class ModBlockEntities {
    public static final BlockEntityEntry<BatchCrafterBlockEntity> BATCH_CRAFTER = REGISTRATOR
        .blockEntity("batch_crafter", BatchCrafterBlockEntity::createBlockEntity)
        .onRegister(BatchCrafterBlockEntity::onBlockEntityRegister)
        .renderer(() -> BatchCrafterRenderer::new)
        .validBlock(ModBlocks.BATCH_CRAFTER)
        .register();

    public static final BlockEntityEntry<AutoCrafterBlockEntity> AUTO_CRAFTER = REGISTRATOR
            .blockEntity("auto_crafter", AutoCrafterBlockEntity::createBlockEntity)
            .onRegister(AutoCrafterBlockEntity::onBlockEntityRegister)
            .validBlock(ModBlocks.AUTO_CRAFTER)
            .register();

    public static final BlockEntityEntry<ItemCollectorBlockEntity> ITEM_COLLECTOR = REGISTRATOR
            .blockEntity("item_collector", ItemCollectorBlockEntity::createBlockEntity)
            .onRegister(ItemCollectorBlockEntity::onBlockEntityRegister)
            .validBlock(ModBlocks.ITEM_COLLECTOR)
            .register();

    public static final BlockEntityEntry<ChuteBlockEntity> CHUTE = REGISTRATOR
        .blockEntity("chute", ChuteBlockEntity::createBlockEntity)
        .onRegister(ChuteBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CHUTE)
        .register();

    public static final BlockEntityEntry<MagneticChuteBlockEntity> MAGNETIC_CHUTE = REGISTRATOR
            .blockEntity("magnetic_chute", MagneticChuteBlockEntity::createBlockEntity)
            .onRegister(MagneticChuteBlockEntity::onBlockEntityRegister)
            .validBlock(ModBlocks.MAGNETIC_CHUTE)
            .register();

    public static final BlockEntityEntry<SimpleChuteBlockEntity> SIMPLE_CHUTE = REGISTRATOR
        .blockEntity("simple_chute", SimpleChuteBlockEntity::createBlockEntity)
        .onRegister(SimpleChuteBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.SIMPLE_CHUTE)
        .register();

    public static final BlockEntityEntry<CrabTrapBlockEntity> CRAB_TRAP = REGISTRATOR
        .blockEntity("crab_trap", CrabTrapBlockEntity::createBlockEntity)
        .onRegister(CrabTrapBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CRAB_TRAP)
        .register();

    public static final BlockEntityEntry<CorruptedBeaconBlockEntity> CORRUPTED_BEACON = REGISTRATOR
        .blockEntity("corrupted_beacon", CorruptedBeaconBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.CORRUPTED_BEACON)
        .renderer(() -> CorruptedBeaconRenderer::new)
        .register();

    public static final BlockEntityEntry<CreativeGeneratorBlockEntity> CREATIVE_GENERATOR = REGISTRATOR
        .blockEntity("creative_generator", CreativeGeneratorBlockEntity::createBlockEntity)
        .renderer(() -> CreativeGeneratorRenderer::new)
        .validBlock(ModBlocks.CREATIVE_GENERATOR)
        .register();

    public static final BlockEntityEntry<HeaterBlockEntity> HEATER = REGISTRATOR
        .blockEntity("heater", HeaterBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.HEATER)
        .register();

    public static final BlockEntityEntry<TransmissionPoleBlockEntity> TRANSMISSION_POLE = REGISTRATOR
        .blockEntity("transmission_pole", TransmissionPoleBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.TRANSMISSION_POLE)
        .register();

    public static final BlockEntityEntry<ChargeCollectorBlockEntity> CHARGE_COLLECTOR = REGISTRATOR
        .blockEntity("charge_collector", ChargeCollectorBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.CHARGE_COLLECTOR)
        .register();

    public static final BlockEntityEntry<MobAmberBlockEntity> MOB_AMBER_BLOCK = REGISTRATOR
        .blockEntity("mob_amber_block", MobAmberBlockEntity::createBlockEntity)
        .renderer(() -> HasMobBlockRenderer::new)
        .validBlock(ModBlocks.MOB_AMBER_BLOCK)
        .register();

    public static final BlockEntityEntry<ResentfulAmberBlock> RESENTFUL_AMBER_BLOCK = REGISTRATOR
        .blockEntity("resentful_amber_block", ResentfulAmberBlock::createBlockEntity)
        .renderer(() -> HasMobBlockRenderer::new)
        .validBlock(ModBlocks.RESENTFUL_AMBER_BLOCK)
        .register();

    public static final BlockEntityEntry<PowerConverterBlockEntity> POWER_CONVERTER = REGISTRATOR
        .blockEntity("power_converter", PowerConverterBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.POWER_CONVERTER_SMALL, ModBlocks.POWER_CONVERTER_MIDDLE, ModBlocks.POWER_CONVERTER_BIG)
        .register();

    public static final BlockEntityEntry<RemoteTransmissionPoleBlockEntity> REMOTE_TRANSMISSION_POLE = REGISTRATOR
        .blockEntity("remote_transmission_pole", RemoteTransmissionPoleBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.REMOTE_TRANSMISSION_POLE)
        .register();

    public static final BlockEntityEntry<LoadMonitorBlockEntity> LOAD_MONITOR = REGISTRATOR
        .blockEntity("load_monitor", LoadMonitorBlockEntity::new)
        .validBlock(ModBlocks.LOAD_MONITOR)
        .register();

    public static final BlockEntityEntry<InductionLightBlockEntity> INDUCTION_LIGHT = REGISTRATOR
            .blockEntity("induction_light", InductionLightBlockEntity::new)
            .validBlock(ModBlocks.INDUCTION_LIGHT)
            .register();

    public static final BlockEntityEntry<OverseerBlockEntity> OVERSEER = REGISTRATOR
        .blockEntity("overseer", OverseerBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.OVERSEER_BLOCK)
        .register();

    public static final BlockEntityEntry<ChargerBlockEntity> CHARGER = REGISTRATOR
            .blockEntity("charger", ChargerBlockEntity::new)
            .validBlock(ModBlocks.CHARGER, ModBlocks.DISCHARGER)
            .onRegister(ChargerBlockEntity::onBlockEntityRegister)
            .register();

    public static final BlockEntityEntry<ActiveSilencerBlockEntity> ACTIVE_SILENCER = REGISTRATOR
            .blockEntity("active_silencer", ActiveSilencerBlockEntity::new)
            .validBlock(ModBlocks.ACTIVE_SILENCER)
            .register();

    public static final BlockEntityEntry<RubyPrismBlockEntity> RUBY_PRISM = REGISTRATOR
        .blockEntity("ruby_prism", RubyPrismBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.RUBY_PRISM)
        .renderer(() -> LaserRenderer::new)
        .register();
    public static final BlockEntityEntry<RubyLaserBlockEntity> RUBY_LASER = REGISTRATOR
        .blockEntity("ruby_laser", RubyLaserBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.RUBY_LASER)
        .renderer(() -> LaserRenderer::new)
        .register();

    public static final BlockEntityEntry<ThermoelectricConverterBlockEntity> THERMOELECTRIC_CONVERTER = REGISTRATOR
            .blockEntity("thermoelectric_converter", ThermoelectricConverterBlockEntity::new)
            .validBlock(ModBlocks.THERMOELECTRIC_CONVERTER)
            .register();

    public static final BlockEntityEntry<MineralFountainBlockEntity> MINERAL_FOUNTAIN = REGISTRATOR
            .blockEntity("mineral_fountain", MineralFountainBlockEntity::createBlockEntity)
            .validBlock(ModBlocks.MINERAL_FOUNTAIN)
            .register();

    public static final BlockEntityEntry<HeliostatsBlockEntity> HELIOSTATS = REGISTRATOR
            .blockEntity("heliostats", HeliostatsBlockEntity::new)
            .validBlock(ModBlocks.HELIOSTATS)
            .renderer(() -> HeliostatsRenderer::new)
            .register();

    public static void register() {
    }
}
