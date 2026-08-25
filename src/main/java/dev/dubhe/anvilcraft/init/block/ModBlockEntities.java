package dev.dubhe.anvilcraft.init.block;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ActivatorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.BlackHoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialBackGateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CrushingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DetectorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HypercubeBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InfiniteCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LaserReceiverBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LensBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LoadMonitorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MassEnergyInverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MobAmberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.NeutronIrradiatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PropelPistonBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RemoteTransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ResentfulAmberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SiftingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleMagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.UnpackingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.VoidEnergyCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.WhiteHoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCutterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.PipeCheckValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.GlowingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatedBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.IncandescentBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.OverheatedBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.RedhotBlockEntity;
import dev.dubhe.anvilcraft.block.entity.plate.TimeCountedPressurePlateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.AdvancedComparatorBlockEntityRender;
import dev.dubhe.anvilcraft.client.renderer.blockentity.AutoEnchantingTableBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BatchCraftingBERenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CelestialForgingAnvilBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ChargeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ChargerBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ConfinementChamberRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ControlValveBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeCrateBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CrushingTableBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.DischargerBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.DrainBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ExpCollectorBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.GlassPipeBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HasMobBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeatCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeliostatsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HypercubeBERenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.InfiniteCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LaserBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.OverseerBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PipeCheckValveBERenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PlasmaJetsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PulseGeneratorBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PumpBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SiftingTableBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SmartBlockPlacerRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SpacetimeSupercomputerBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.StampingPlatformBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.TeslaTowerRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.TradingStationBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.UnpackingTableBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.VoidEnergyCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.WipBlockEntityRenderer;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

public class ModBlockEntities {
    public static final BlockEntityEntry<BatchCrafterBlockEntity> BATCH_CRAFTER = REGISTRUM.blockEntity(
        "batch_crafter",
        BatchCrafterBlockEntity::new
    ).renderer(() -> BatchCraftingBERenderer::new).validBlock(ModBlocks.BATCH_CRAFTER).register();

    public static final BlockEntityEntry<BatchCutterBlockEntity> BATCH_CUTTER = REGISTRUM.blockEntity(
        "batch_cutter",
        BatchCutterBlockEntity::new
    ).renderer(() -> BatchCraftingBERenderer::new).validBlock(ModBlocks.BATCH_CUTTER).register();

    public static final BlockEntityEntry<ItemCollectorBlockEntity> ITEM_COLLECTOR = REGISTRUM.blockEntity(
        "item_collector",
        ItemCollectorBlockEntity::new
    ).validBlock(ModBlocks.ITEM_COLLECTOR).register();

    public static final BlockEntityEntry<ItemDetectorBlockEntity> ITEM_DETECTOR = REGISTRUM.blockEntity(
        "item_detector",
        ItemDetectorBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.ITEM_DETECTOR).register();

    public static final BlockEntityEntry<AutoEnchantingTableBlockEntity> AUTO_ENCHANTING_TABLE = REGISTRUM.blockEntity(
        "auto_enchanting_table",
        AutoEnchantingTableBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.AUTO_ENCHANTING_TABLE)
        .renderer(() -> AutoEnchantingTableBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<ChuteBlockEntity> CHUTE = REGISTRUM.blockEntity("chute", ChuteBlockEntity::createBlockEntity)
        .onRegister(ChuteBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CHUTE)
        .register();

    public static final BlockEntityEntry<MagneticChuteBlockEntity> MAGNETIC_CHUTE = REGISTRUM.blockEntity(
        "magnetic_chute",
        MagneticChuteBlockEntity::new
    ).validBlock(ModBlocks.MAGNETIC_CHUTE).register();

    public static final BlockEntityEntry<SimpleChuteBlockEntity> SIMPLE_CHUTE = REGISTRUM.blockEntity(
        "simple_chute",
        SimpleChuteBlockEntity::new
    ).validBlock(ModBlocks.SIMPLE_CHUTE).register();

    public static final BlockEntityEntry<SimpleMagneticChuteBlockEntity> SIMPLE_MAGNETIC_CHUTE = REGISTRUM.blockEntity(
        "simple_magnetic_chute",
        SimpleMagneticChuteBlockEntity::new
    ).validBlock(ModBlocks.SIMPLE_MAGNETIC_CHUTE).register();

    public static final BlockEntityEntry<CorruptedBeaconBlockEntity> CORRUPTED_BEACON = REGISTRUM.blockEntity(
        "corrupted_beacon",
        CorruptedBeaconBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.CORRUPTED_BEACON).renderer(() -> CorruptedBeaconRenderer::new).register();

    public static final BlockEntityEntry<CreativeGeneratorBlockEntity> CREATIVE_GENERATOR = REGISTRUM.blockEntity(
        "creative_generator",
        CreativeGeneratorBlockEntity::createBlockEntity
    ).renderer(() -> CreativeGeneratorRenderer::new).validBlock(ModBlocks.CREATIVE_GENERATOR).register();

    public static final BlockEntityEntry<CreativeLaserBlockEntity> CREATIVE_LASER = REGISTRUM.blockEntity(
        "creative_laser",
        CreativeLaserBlockEntity::createBlockEntity
    ).renderer(() -> LaserBlockRenderer::new).validBlock(ModBlocks.CREATIVE_LASER).register();

    public static final BlockEntityEntry<HeaterBlockEntity> HEATER = REGISTRUM.blockEntity("heater", HeaterBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.HEATER)
        .register();

    public static final BlockEntityEntry<BurningHeaterBlockEntity> BURNING_HEATER = REGISTRUM.blockEntity(
        "burning_heater",
        BurningHeaterBlockEntity::new
    ).validBlock(ModBlocks.BURNING_HEATER).register();

    public static final BlockEntityEntry<TransmissionPoleBlockEntity> TRANSMISSION_POLE = REGISTRUM.blockEntity(
        "transmission_pole",
        TransmissionPoleBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.TRANSMISSION_POLE).register();

    public static final BlockEntityEntry<ChargeCollectorBlockEntity> CHARGE_COLLECTOR = REGISTRUM.blockEntity(
        "charge_collector",
        ChargeCollectorBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.CHARGE_COLLECTOR).renderer(() -> ChargeCollectorRenderer::new).register();

    public static final BlockEntityEntry<FeCollectorBlockEntity> FE_COLLECTOR = REGISTRUM.blockEntity(
        "fe_collector",
        FeCollectorBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.FE_COLLECTOR).renderer(() -> FeCollectorRenderer::new).register();

    public static final BlockEntityEntry<MobAmberBlockEntity> MOB_AMBER_BLOCK = REGISTRUM.blockEntity(
        "mob_amber_block",
        MobAmberBlockEntity::createBlockEntity
    ).renderer(() -> HasMobBlockRenderer::new).validBlock(ModBlocks.MOB_AMBER_BLOCK).register();

    public static final BlockEntityEntry<ResentfulAmberBlockEntity> RESENTFUL_AMBER_BLOCK = REGISTRUM.blockEntity(
        "resentful_amber_block",
        ResentfulAmberBlockEntity::createBlockEntity
    ).renderer(() -> HasMobBlockRenderer::new).validBlock(ModBlocks.RESENTFUL_AMBER_BLOCK).register();

    public static final BlockEntityEntry<PowerConverterBlockEntity> POWER_CONVERTER = REGISTRUM.blockEntity(
        "power_converter",
        PowerConverterBlockEntity::createBlockEntity
    ).validBlocks(
        ModBlocks.POWER_CONVERTER_SMALL,
        ModBlocks.POWER_CONVERTER_MIDDLE,
        ModBlocks.POWER_CONVERTER_BIG,
        ModBlocks.POWER_CONVERTER_SUPER_BIG,
        ModBlocks.POWER_CONVERTER_EXTREMELY_BIG
    ).register();

    public static final BlockEntityEntry<RemoteTransmissionPoleBlockEntity> REMOTE_TRANSMISSION_POLE = REGISTRUM.blockEntity(
        "remote_transmission_pole",
        RemoteTransmissionPoleBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.REMOTE_TRANSMISSION_POLE).register();

    public static final BlockEntityEntry<LoadMonitorBlockEntity> LOAD_MONITOR = REGISTRUM.blockEntity(
        "load_monitor",
        LoadMonitorBlockEntity::new
    ).validBlock(ModBlocks.LOAD_MONITOR).register();

    public static final BlockEntityEntry<InductionLightBlockEntity> INDUCTION_LIGHT = REGISTRUM.blockEntity(
        "induction_light",
        InductionLightBlockEntity::new
    ).validBlock(ModBlocks.INDUCTION_LIGHT).register();

    public static final BlockEntityEntry<LensBlockEntity> LENS = REGISTRUM.blockEntity(
        "lens",
        LensBlockEntity::new
    ).validBlock(ModBlocks.LENS).register();

    public static final BlockEntityEntry<OverseerBlockEntity> OVERSEER = REGISTRUM.blockEntity(
        "overseer",
        OverseerBlockEntity::createBlockEntity
    ).renderer(() -> OverseerBlockEntityRenderer::new).validBlock(ModBlocks.OVERSEER_BLOCK).register();

    public static final BlockEntityEntry<ChargerBlockEntity> CHARGER = REGISTRUM.blockEntity("charger", ChargerBlockEntity::new)
        .renderer(() -> ChargerBlockRenderer::new)
        .validBlock(ModBlocks.CHARGER)
        .register();

    public static final BlockEntityEntry<DischargerBlockEntity> DISCHARGER = REGISTRUM.blockEntity("discharger", DischargerBlockEntity::new)
        .renderer(() -> DischargerBlockRenderer::new)
        .validBlock(ModBlocks.DISCHARGER)
        .register();

    public static final BlockEntityEntry<ActiveSilencerBlockEntity> ACTIVE_SILENCER = REGISTRUM.blockEntity(
        "active_silencer",
        ActiveSilencerBlockEntity::new
    ).validBlocks(ModBlocks.ACTIVE_SILENCER).register();

    public static final BlockEntityEntry<RubyPrismBlockEntity> RUBY_PRISM = REGISTRUM.blockEntity(
        "ruby_prism",
        RubyPrismBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.RUBY_PRISM).renderer(() -> LaserBlockRenderer::new).register();
    public static final BlockEntityEntry<RubyLaserBlockEntity> RUBY_LASER = REGISTRUM.blockEntity(
        "ruby_laser",
        RubyLaserBlockEntity::createBlockEntity
    ).validBlock(ModBlocks.RUBY_LASER).renderer(() -> LaserBlockRenderer::new).register();

    public static final BlockEntityEntry<HeatCollectorBlockEntity> HEAT_COLLECTOR = REGISTRUM.blockEntity(
        "heat_collector",
        HeatCollectorBlockEntity::createBlockEntity
    ).renderer(() -> HeatCollectorRenderer::new).validBlock(ModBlocks.HEAT_COLLECTOR).register();

    public static final BlockEntityEntry<InfiniteCollectorBlockEntity> INFINITE_COLLECTOR = REGISTRUM.blockEntity(
        "infinite_collector",
        InfiniteCollectorBlockEntity::createBlockEntity
    ).renderer(() -> InfiniteCollectorRenderer::new).validBlock(ModBlocks.INFINITE_COLLECTOR).register();

    public static final BlockEntityEntry<MineralFountainBlockEntity> MINERAL_FOUNTAIN = REGISTRUM.blockEntity(
        "mineral_fountain",
        MineralFountainBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.MINERAL_FOUNTAIN).register();

    public static final BlockEntityEntry<HeliostatsBlockEntity> HELIOSTATS = REGISTRUM.blockEntity(
        "heliostats",
        HeliostatsBlockEntity::new
    ).validBlocks(ModBlocks.HELIOSTATS).renderer(() -> HeliostatsRenderer::new).register();

    public static final BlockEntityEntry<TeslaTowerBlockEntity> TESLA_TOWER = REGISTRUM.blockEntity(
        "tesla_tower",
        TeslaTowerBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.TESLA_TOWER).renderer(() -> TeslaTowerRenderer::new).register();

    public static final BlockEntityEntry<SpaceOvercompressorBlockEntity> SPACE_OVERCOMPRESSOR = REGISTRUM.blockEntity(
        "space_overcompressor",
        SpaceOvercompressorBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.SPACE_OVERCOMPRESSOR).register();

    public static final BlockEntityEntry<MassEnergyInverterBlockEntity> MASS_ENERGY_INVERTER = REGISTRUM.blockEntity(
        "mass_energy_inverter",
        MassEnergyInverterBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.MASS_ENERGY_INVERTER).register();

    public static final BlockEntityEntry<BlackHoleBlockEntity> BLACK_HOLE = REGISTRUM.blockEntity(
        "black_hole",
        BlackHoleBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.BLACK_HOLE).register();

    public static final BlockEntityEntry<WhiteHoleBlockEntity> WHITE_HOLE = REGISTRUM.blockEntity(
        "white_hole",
        WhiteHoleBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.WHITE_HOLE).register();

    public static final BlockEntityEntry<TimeCountedPressurePlateBlockEntity> TIME_COUNTED_PRESSURE_PLATE = REGISTRUM.blockEntity(
        "time_counted_pressure_plate",
        TimeCountedPressurePlateBlockEntity::createBlockEntity
    ).validBlocks(
        ModBlocks.COPPER_PRESSURE_PLATE,
        ModBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
        ModBlocks.WEATHERED_COPPER_PRESSURE_PLATE,
        ModBlocks.OXIDIZED_COPPER_PRESSURE_PLATE
    ).register();

    public static final BlockEntityEntry<AccelerationRingBlockEntity> ACCELERATION_RING = REGISTRUM.blockEntity(
        "acceleration_ring",
        AccelerationRingBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.ACCELERATION_RING).register();

    public static final BlockEntityEntry<DeflectionRingBlockEntity> DEFLECTION_RING = REGISTRUM.blockEntity(
        "deflection_ring",
        DeflectionRingBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.DEFLECTION_RING).register();

    public static final BlockEntityEntry<ConfinementChamberBlockEntity> CONFINEMENT_CHAMBER = REGISTRUM.blockEntity(
        "confinement_chamber",
        ConfinementChamberBlockEntity::createBlockEntity
    ).renderer(() -> ConfinementChamberRenderer::new).validBlocks(ModBlocks.CONFINEMENT_CHAMBER).register();

    public static final BlockEntityEntry<VoidEnergyCollectorBlockEntity> VOID_ENERGY_COLLECTOR = REGISTRUM.blockEntity(
        "void_energy_collector",
        VoidEnergyCollectorBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.VOID_ENERGY_COLLECTOR).renderer(() -> VoidEnergyCollectorRenderer::new).register();

    public static final BlockEntityEntry<PulseGeneratorBlockEntity> PULSE_GENERATOR = REGISTRUM.blockEntity(
        "pulse_generator",
        PulseGeneratorBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.PULSE_GENERATOR).renderer(() -> PulseGeneratorBlockEntityRenderer::new).register();

    public static final BlockEntityEntry<AdvancedComparatorBlockEntity> ADVANCED_COMPARATOR = REGISTRUM.blockEntity(
        "advanced_comparator",
        AdvancedComparatorBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.ADVANCED_COMPARATOR).renderer(() -> AdvancedComparatorBlockEntityRender::new).register();

    public static final BlockEntityEntry<HeatedBlockEntity> HEATED_BLOCK = REGISTRUM.blockEntity(
        "heated_block",
        HeatedBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.HEATED_NETHERITE_BLOCK, ModBlocks.HEATED_TUNGSTEN_BLOCK).register();
    public static final BlockEntityEntry<RedhotBlockEntity> REDHOT_BLOCK = REGISTRUM.blockEntity(
        "redhot_block",
        RedhotBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.REDHOT_NETHERITE_BLOCK, ModBlocks.REDHOT_TUNGSTEN_BLOCK).register();
    public static final BlockEntityEntry<GlowingBlockEntity> GLOWING_BLOCK = REGISTRUM.blockEntity(
        "glowing_block",
        GlowingBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.GLOWING_NETHERITE_BLOCK, ModBlocks.GLOWING_TUNGSTEN_BLOCK).register();
    public static final BlockEntityEntry<IncandescentBlockEntity> INCANDESCENT_BLOCK = REGISTRUM.blockEntity(
        "incandescent_block",
        IncandescentBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.INCANDESCENT_NETHERITE_BLOCK, ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK).register();
    public static final BlockEntityEntry<OverheatedBlockEntity> OVERHEATED_BLOCK = REGISTRUM.blockEntity(
        "overheated_block",
        OverheatedBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK).register();

    public static final BlockEntityEntry<PlasmaJetsBlockEntity> PLASMA_JETS = REGISTRUM.blockEntity(
        "plasma_jets",
        PlasmaJetsBlockEntity::createBlockEntity
    ).validBlocks(ModBlocks.PLASMA_JETS).renderer(() -> PlasmaJetsRenderer::new).register();

    public static final BlockEntityEntry<DetectorSlidingRailBlockEntity> DETECTOR_SLIDING_RAIL = REGISTRUM.blockEntity(
        "detector_sliding_rail",
        DetectorSlidingRailBlockEntity::new
    ).validBlocks(ModBlocks.DETECTOR_SLIDING_RAIL).register();

    public static final BlockEntityEntry<ActivatorSlidingRailBlockEntity> ACTIVATOR_SLIDING_RAIL = REGISTRUM.blockEntity(
        "activator_sliding_rail",
        ActivatorSlidingRailBlockEntity::new
    ).validBlocks(ModBlocks.ACTIVATOR_SLIDING_RAIL).register();

    public static final BlockEntityEntry<PropelPistonBlockEntity> PROPEL_PISTON = REGISTRUM
        .blockEntity("propel_piston", PropelPistonBlockEntity::new)
        .validBlock(ModBlocks.PROPEL_PISTON)
        .register();

    public static final BlockEntityEntry<LaserReceiverBlockEntity> LASER_RECEIVER = REGISTRUM
        .blockEntity("laser_receiver", LaserReceiverBlockEntity::new)
        .validBlocks(ModBlocks.LASER_RECEIVER)
        .register();

    public static final BlockEntityEntry<NeutronIrradiatorBlockEntity> NEUTRON_IRRADIATOR = REGISTRUM
        .blockEntity("neutron_irradiator", NeutronIrradiatorBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.NEUTRON_IRRADIATOR)
        .register();

    public static final BlockEntityEntry<CrateBlockEntity> CRATE = REGISTRUM
        .blockEntity("crate", CrateBlockEntity::new)
        .validBlocks(ModBlocks.CRATE)
        .register();

    public static final BlockEntityEntry<LargeCrateBlockEntity> LARGE_CRATE = REGISTRUM
        .blockEntity("large_crate", LargeCrateBlockEntity::new)
        .validBlocks(ModBlocks.LARGE_CRATE)
        .register();

    public static final BlockEntityEntry<ShulkerContainerBlockEntity> SHULKER_CONTAINER = REGISTRUM
        .blockEntity("shulker_container", ShulkerContainerBlockEntity::new)
        .validBlocks(ModBlocks.SHULKER_CONTAINER)
        .register();

    public static final BlockEntityEntry<HyperdimensionStorageStationBlockEntity> HYPERDIMENSION_STORAGE_STATION = REGISTRUM
        .blockEntity("hyperdimension_storage_station", HyperdimensionStorageStationBlockEntity::new)
        .validBlocks(ModBlocks.HYPERDIMENSION_STORAGE_STATION)
        .register();

    public static final BlockEntityEntry<FluidTankBlockEntity> FLUID_TANK = REGISTRUM
        .blockEntity("fluid_tank", FluidTankBlockEntity::new)
        .validBlocks(ModBlocks.FLUID_TANK)
        .renderer(() -> FluidTankBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<LargeFluidTankBlockEntity> LARGE_FLUID_TANK = REGISTRUM
        .blockEntity("large_fluid_tank", LargeFluidTankBlockEntity::new)
        .validBlocks(ModBlocks.LARGE_FLUID_TANK)
        .renderer(() -> LargeFluidTankBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<LargeCauldronBlockEntity> LARGE_CAULDRON = REGISTRUM
        .blockEntity("large_cauldron", LargeCauldronBlockEntity::new)
        .validBlocks(ModBlocks.LARGE_CAULDRON)
        .renderer(() -> LargeCauldronBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<CelestialForgingAnvilBlockEntity> CELESTIAL_FORGING_ANVIL = REGISTRUM
        .blockEntity("celestial_forging_anvil", CelestialForgingAnvilBlockEntity::new)
        .validBlock(ModBlocks.CELESTIAL_FORGING_ANVIL)
        .renderer(() -> CelestialForgingAnvilBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<CelestialForgingAnvilLogisticsInterfaceBlockEntity>
        CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE = REGISTRUM
        .blockEntity("celestial_forging_anvil_logistics_interface",
            CelestialForgingAnvilLogisticsInterfaceBlockEntity::new)
        .validBlock(ModBlocks.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE)
        .register();

    public static final BlockEntityEntry<CelestialForgingAnvilFluidInterfaceBlockEntity>
        CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE = REGISTRUM
        .blockEntity("celestial_forging_anvil_fluid_interface",
            CelestialForgingAnvilFluidInterfaceBlockEntity::new)
        .validBlock(ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE)
        .register();

    public static final BlockEntityEntry<CelestialForgingAnvilLaserInterfaceBlockEntity>
        CELESTIAL_FORGING_ANVIL_LASER_INTERFACE = REGISTRUM
        .blockEntity("celestial_forging_anvil_laser_interface",
            CelestialForgingAnvilLaserInterfaceBlockEntity::new)
        .validBlock(ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE)
        .renderer(() -> LaserBlockRenderer::new)
        .register();

    public static final BlockEntityEntry<CelestialForgingAnvilPortalBlockEntity>
        CELESTIAL_FORGING_ANVIL_PORTAL = REGISTRUM
        .blockEntity("celestial_forging_anvil_portal",
            CelestialForgingAnvilPortalBlockEntity::new)
        .validBlock(ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL)
        .renderer(() -> LaserBlockRenderer::new)
        .register();

    public static final BlockEntityEntry<CelestialBackGateBlockEntity> CELESTIAL_BACK_GATE = REGISTRUM
        .blockEntity("celestial_back_gate", CelestialBackGateBlockEntity::new)
        .validBlock(ModBlocks.CELESTIAL_BACK_GATE)
        .register();

    public static final BlockEntityEntry<TradingStationBlockEntity> TRADING_STATION = REGISTRUM
        .blockEntity("trading_station", TradingStationBlockEntity::new)
        .validBlock(ModBlocks.TRADING_STATION)
        .renderer(() -> TradingStationBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<FishTankBlockEntity> FISH_TANK = REGISTRUM
        .blockEntity("fish_tank", FishTankBlockEntity::new)
        .validBlocks(ModBlocks.FISH_TANK)
        .renderer(() -> FishTankBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<CrushingTableBlockEntity> CRUSHING_TABLE = REGISTRUM
        .blockEntity("crushing_table", CrushingTableBlockEntity::new)
        .validBlocks(ModBlocks.CRUSHING_TABLE)
        .renderer(() -> CrushingTableBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<StampingPlatformBlockEntity> STAMPING_PLATFORM = REGISTRUM
        .blockEntity("stamping_platform", StampingPlatformBlockEntity::new)
        .validBlocks(ModBlocks.STAMPING_PLATFORM)
        .renderer(() -> StampingPlatformBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<SiftingTableBlockEntity> SIFTING_TABLE = REGISTRUM
        .blockEntity("sifting_table", SiftingTableBlockEntity::new)
        .validBlocks(ModBlocks.SIFTING_TABLE)
        .renderer(() -> SiftingTableBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<UnpackingTableBlockEntity> UNPACKING_TABLE = REGISTRUM
        .blockEntity("unpacking_table", UnpackingTableBlockEntity::new)
        .validBlocks(ModBlocks.UNPACKING_TABLE)
        .renderer(() -> UnpackingTableBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<ExpCollectorBlockEntity> EXP_COLLECTOR = REGISTRUM
        .blockEntity("exp_collector", ExpCollectorBlockEntity::new)
        .validBlocks(ModBlocks.EXP_COLLECTOR)
        .renderer(() -> ExpCollectorBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<SmartBlockPlacerBlockEntity> SMART_BLOCK_PLACER = REGISTRUM
        .blockEntity("smart_block_placer", SmartBlockPlacerBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.SMART_BLOCK_PLACER)
        .renderer(() -> SmartBlockPlacerRenderer::new)
        .register();

    public static final BlockEntityEntry<StructureScannerBlockEntity> STRUCTURE_SCANNER = REGISTRUM
        .blockEntity("structure_scanner", StructureScannerBlockEntity::new)
        .validBlock(ModBlocks.STRUCTURE_SCANNER)
        .register();

    public static final BlockEntityEntry<SpacetimeSupercomputerBlockEntity> SPACETIME_SUPERCOMPUTER = REGISTRUM
        .blockEntity("spacetime_supercomputer", SpacetimeSupercomputerBlockEntity::new)
        .renderer(() -> SpacetimeSupercomputerBlockEntityRenderer::new)
        .validBlock(ModBlocks.SPACETIME_SUPERCOMPUTER)
        .register();

    public static final BlockEntityEntry<LargeLaserBlockEntity> LARGE_LASER = REGISTRUM
        .blockEntity("large_laser", LargeLaserBlockEntity::new)
        .validBlock(ModBlocks.LARGE_LASER)
        .renderer(() -> LaserBlockRenderer::new)
        .register();

    public static final BlockEntityEntry<PumpBlockEntity> PUMP = REGISTRUM
        .blockEntity("pump", PumpBlockEntity::create)
        .validBlock(ModBlocks.PUMP)
        .renderer(() -> PumpBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<ControlValveBlockEntity> CONTROL_VALVE = REGISTRUM
        .blockEntity("control_valve", ControlValveBlockEntity::new)
        .validBlock(ModBlocks.CONTROL_VALVE)
        .renderer(() -> ControlValveBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<PipeCheckValveBlockEntity> PIPE_CHECK_VALVE = REGISTRUM
        .blockEntity("pipe_check_valve", PipeCheckValveBlockEntity::new)
        .validBlocks(
            ModBlocks.PIPE_STRAIGHT,
            ModBlocks.PIPE_CORNER,
            ModBlocks.PIPE_NODE
        )
        .renderer(() -> PipeCheckValveBERenderer::new)
        .register();

    public static final BlockEntityEntry<GlassPipeBlockEntity> GLASS_PIPE = REGISTRUM
        .blockEntity("glass_pipe", GlassPipeBlockEntity::new)
        .validBlocks(
            ModBlocks.GLASS_PIPE_STRAIGHT,
            ModBlocks.GLASS_PIPE_CORNER,
            ModBlocks.GLASS_PIPE_NODE
        )
        .renderer(() -> GlassPipeBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<DrainBlockEntity> DRAIN = REGISTRUM
        .blockEntity("drain", DrainBlockEntity::new)
        .validBlock(ModBlocks.DRAIN)
        .renderer(() -> DrainBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<WipBlockEntity> WIP_BLOCK = REGISTRUM
        .blockEntity("wip_block", WipBlockEntity::new)
        .validBlock(ModBlocks.WIP_BLOCK)
        .renderer(() -> WipBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<CreativeFluidTankBlockEntity> CREATIVE_FLUID_TANK = REGISTRUM
        .blockEntity("creative_fluid_tank", CreativeFluidTankBlockEntity::new)
        .validBlock(ModBlocks.CREATIVE_FLUID_TANK)
        .renderer(() -> CreativeFluidTankBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<CreativeCrateBlockEntity> CREATIVE_CRATE = REGISTRUM
        .blockEntity("creative_crate", CreativeCrateBlockEntity::new)
        .validBlock(ModBlocks.CREATIVE_CRATE)
        .renderer(() -> CreativeCrateBlockEntityRenderer::new)
        .register();

    public static final BlockEntityEntry<HypercubeBlockEntity> HYPERCUBE = REGISTRUM
        .blockEntity("hypercube", HypercubeBlockEntity::new)
        .validBlock(ModBlocks.HYPERCUBE)
        .renderer(() -> HypercubeBERenderer::new)
        .register();

    public static void register() {
    }
}
