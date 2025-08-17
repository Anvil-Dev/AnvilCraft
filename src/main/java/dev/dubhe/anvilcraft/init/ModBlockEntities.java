package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ActivatorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DetectorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LoadMonitorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MobAmberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RemoteTransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ResentfulAmberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.VoidEnergyCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.GlowingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatedBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.IncandescentBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.OverheatedBlockEntity;
import dev.dubhe.anvilcraft.block.entity.heatable.RedhotBlockEntity;
import dev.dubhe.anvilcraft.block.entity.plate.TimeCountedPressurePlateBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BatchCrafterRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ChargeCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.ConfinementChamberRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeGeneratorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HasMobBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeatCollectorRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeliostatsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LaserBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PlasmaJetsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.VoidEnergyCollectorRenderer;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModBlockEntities {
    public static final BlockEntityEntry<BatchCrafterBlockEntity> BATCH_CRAFTER = REGISTRATE
        .blockEntity("batch_crafter", BatchCrafterBlockEntity::new)
        .renderer(() -> BatchCrafterRenderer::new)
        .validBlock(ModBlocks.BATCH_CRAFTER)
        .register();

    public static final BlockEntityEntry<ItemCollectorBlockEntity> ITEM_COLLECTOR = REGISTRATE
        .blockEntity("item_collector", ItemCollectorBlockEntity::new)
        .validBlock(ModBlocks.ITEM_COLLECTOR)
        .register();

    public static final BlockEntityEntry<ItemDetectorBlockEntity> ITEM_DETECTOR = REGISTRATE
        .blockEntity("item_detector", ItemDetectorBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.ITEM_DETECTOR)
        .register();

    public static final BlockEntityEntry<ChuteBlockEntity> CHUTE = REGISTRATE
        .blockEntity("chute", ChuteBlockEntity::createBlockEntity)
        .onRegister(ChuteBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CHUTE)
        .register();

    public static final BlockEntityEntry<MagneticChuteBlockEntity> MAGNETIC_CHUTE = REGISTRATE
        .blockEntity("magnetic_chute", MagneticChuteBlockEntity::new)
        .validBlock(ModBlocks.MAGNETIC_CHUTE)
        .register();

    public static final BlockEntityEntry<SimpleChuteBlockEntity> SIMPLE_CHUTE = REGISTRATE
        .blockEntity("simple_chute", SimpleChuteBlockEntity::new)
        .validBlock(ModBlocks.SIMPLE_CHUTE)
        .register();

    public static final BlockEntityEntry<CrabTrapBlockEntity> CRAB_TRAP = REGISTRATE
        .blockEntity("crab_trap", CrabTrapBlockEntity::new)
        .validBlock(ModBlocks.CRAB_TRAP)
        .register();

    public static final BlockEntityEntry<CorruptedBeaconBlockEntity> CORRUPTED_BEACON = REGISTRATE
        .blockEntity("corrupted_beacon", CorruptedBeaconBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.CORRUPTED_BEACON)
        .renderer(() -> CorruptedBeaconRenderer::new)
        .register();

    public static final BlockEntityEntry<CreativeGeneratorBlockEntity> CREATIVE_GENERATOR = REGISTRATE
        .blockEntity("creative_generator", CreativeGeneratorBlockEntity::createBlockEntity)
        .renderer(() -> CreativeGeneratorRenderer::new)
        .validBlock(ModBlocks.CREATIVE_GENERATOR)
        .register();

    public static final BlockEntityEntry<HeaterBlockEntity> HEATER = REGISTRATE
        .blockEntity("heater", HeaterBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.HEATER)
        .register();

    public static final BlockEntityEntry<TransmissionPoleBlockEntity> TRANSMISSION_POLE = REGISTRATE
        .blockEntity("transmission_pole", TransmissionPoleBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.TRANSMISSION_POLE)
        .register();

    public static final BlockEntityEntry<ChargeCollectorBlockEntity> CHARGE_COLLECTOR = REGISTRATE
        .blockEntity("charge_collector", ChargeCollectorBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.CHARGE_COLLECTOR)
        .renderer(() -> ChargeCollectorRenderer::new)
        .register();

    public static final BlockEntityEntry<MobAmberBlockEntity> MOB_AMBER_BLOCK = REGISTRATE
        .blockEntity("mob_amber_block", MobAmberBlockEntity::createBlockEntity)
        .renderer(() -> HasMobBlockRenderer::new)
        .validBlock(ModBlocks.MOB_AMBER_BLOCK)
        .register();

    public static final BlockEntityEntry<ResentfulAmberBlockEntity> RESENTFUL_AMBER_BLOCK = REGISTRATE
        .blockEntity("resentful_amber_block", ResentfulAmberBlockEntity::createBlockEntity)
        .renderer(() -> HasMobBlockRenderer::new)
        .validBlock(ModBlocks.RESENTFUL_AMBER_BLOCK)
        .register();

    public static final BlockEntityEntry<PowerConverterBlockEntity> POWER_CONVERTER = REGISTRATE
        .blockEntity("power_converter", PowerConverterBlockEntity::createBlockEntity)
        .validBlocks(
            ModBlocks.POWER_CONVERTER_SMALL, ModBlocks.POWER_CONVERTER_MIDDLE, ModBlocks.POWER_CONVERTER_BIG)
        .register();

    public static final BlockEntityEntry<RemoteTransmissionPoleBlockEntity> REMOTE_TRANSMISSION_POLE = REGISTRATE
        .blockEntity("remote_transmission_pole", RemoteTransmissionPoleBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.REMOTE_TRANSMISSION_POLE)
        .register();

    public static final BlockEntityEntry<LoadMonitorBlockEntity> LOAD_MONITOR = REGISTRATE
        .blockEntity("load_monitor", LoadMonitorBlockEntity::new)
        .validBlock(ModBlocks.LOAD_MONITOR)
        .register();

    public static final BlockEntityEntry<InductionLightBlockEntity> INDUCTION_LIGHT = REGISTRATE
        .blockEntity("induction_light", InductionLightBlockEntity::new)
        .validBlock(ModBlocks.INDUCTION_LIGHT)
        .register();

    public static final BlockEntityEntry<OverseerBlockEntity> OVERSEER = REGISTRATE
        .blockEntity("overseer", OverseerBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.OVERSEER_BLOCK)
        .register();

    public static final BlockEntityEntry<ChargerBlockEntity> CHARGER = REGISTRATE
        .blockEntity("charger", ChargerBlockEntity::new)
        .validBlocks(ModBlocks.CHARGER, ModBlocks.DISCHARGER)
        .register();

    public static final BlockEntityEntry<ActiveSilencerBlockEntity> ACTIVE_SILENCER = REGISTRATE
        .blockEntity("active_silencer", ActiveSilencerBlockEntity::new)
        .validBlocks(ModBlocks.ACTIVE_SILENCER)
        .register();

    public static final BlockEntityEntry<RubyPrismBlockEntity> RUBY_PRISM = REGISTRATE
        .blockEntity("ruby_prism", RubyPrismBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.RUBY_PRISM)
        .renderer(() -> LaserBlockRenderer::new)
        .register();
    public static final BlockEntityEntry<RubyLaserBlockEntity> RUBY_LASER = REGISTRATE
        .blockEntity("ruby_laser", RubyLaserBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.RUBY_LASER)
        .renderer(() -> LaserBlockRenderer::new)
        .register();

    public static final BlockEntityEntry<HeatCollectorBlockEntity> HEAT_COLLECTOR = REGISTRATE
        .blockEntity("heat_collector", HeatCollectorBlockEntity::createBlockEntity)
        .renderer(() -> HeatCollectorRenderer::new)
        .validBlock(ModBlocks.HEAT_COLLECTOR)
        .register();

    public static final BlockEntityEntry<MineralFountainBlockEntity> MINERAL_FOUNTAIN = REGISTRATE
        .blockEntity("mineral_fountain", MineralFountainBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.MINERAL_FOUNTAIN)
        .register();

    public static final BlockEntityEntry<HeliostatsBlockEntity> HELIOSTATS = REGISTRATE
        .blockEntity("heliostats", HeliostatsBlockEntity::new)
        .validBlocks(ModBlocks.HELIOSTATS)
        .renderer(() -> HeliostatsRenderer::new)
        .register();

    public static final BlockEntityEntry<TeslaTowerBlockEntity> TESLA_TOWER = REGISTRATE
        .blockEntity("tesla_tower", TeslaTowerBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.TESLA_TOWER)
        .register();

    public static final BlockEntityEntry<SpaceOvercompressorBlockEntity> SPACE_OVERCOMPRESSOR = REGISTRATE
        .blockEntity("space_overcompressor", SpaceOvercompressorBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.SPACE_OVERCOMPRESSOR)
        .register();

    public static final BlockEntityEntry<TimeCountedPressurePlateBlockEntity> TIME_COUNTED_PRESSURE_PLATE = REGISTRATE
        .blockEntity("time_counted_pressure_plate", TimeCountedPressurePlateBlockEntity::createBlockEntity)
        .validBlocks(
            ModBlocks.COPPER_PRESSURE_PLATE,
            ModBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
            ModBlocks.WEATHERED_COPPER_PRESSURE_PLATE,
            ModBlocks.OXIDIZED_COPPER_PRESSURE_PLATE
        )
        .register();

    public static final BlockEntityEntry<AccelerationRingBlockEntity> ACCELERATION_RING = REGISTRATE
        .blockEntity("acceleration_ring", AccelerationRingBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.ACCELERATION_RING)
        .register();

    public static final BlockEntityEntry<DeflectionRingBlockEntity> DEFLECTION_RING = REGISTRATE
        .blockEntity("deflection_ring", DeflectionRingBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.DEFLECTION_RING)
        .register();

    public static final BlockEntityEntry<ConfinementChamberBlockEntity> CONFINEMENT_CHAMBER = REGISTRATE
        .blockEntity("confinement_chamber", ConfinementChamberBlockEntity::createBlockEntity)
        .renderer(() -> ConfinementChamberRenderer::new)
        .validBlocks(ModBlocks.CONFINEMENT_CHAMBER)
        .register();

    public static final BlockEntityEntry<VoidEnergyCollectorBlockEntity> VOID_ENERGY_COLLECTOR = REGISTRATE
        .blockEntity("void_energy_collector", VoidEnergyCollectorBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.VOID_ENERGY_COLLECTOR)
        .renderer(() -> VoidEnergyCollectorRenderer::new)
        .register();

    public static final BlockEntityEntry<PulseGeneratorBlockEntity> PULSE_GENERATOR = REGISTRATE
        .blockEntity("pulse_generator", PulseGeneratorBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.PULSE_GENERATOR)
        .register();

    public static final BlockEntityEntry<HeatedBlockEntity> HEATED_BLOCK = REGISTRATE
        .blockEntity("heated_block", HeatedBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.HEATED_NETHERITE_BLOCK, ModBlocks.HEATED_TUNGSTEN_BLOCK)
        .register();
    public static final BlockEntityEntry<RedhotBlockEntity> REDHOT_BLOCK = REGISTRATE
        .blockEntity("redhot_block", RedhotBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.REDHOT_NETHERITE_BLOCK, ModBlocks.REDHOT_TUNGSTEN_BLOCK)
        .register();
    public static final BlockEntityEntry<GlowingBlockEntity> GLOWING_BLOCK = REGISTRATE
        .blockEntity("glowing_block", GlowingBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.GLOWING_NETHERITE_BLOCK, ModBlocks.GLOWING_TUNGSTEN_BLOCK)
        .register();
    public static final BlockEntityEntry<IncandescentBlockEntity> INCANDESCENT_BLOCK = REGISTRATE
        .blockEntity("incandescent_block", IncandescentBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.INCANDESCENT_NETHERITE_BLOCK, ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK)
        .register();
    public static final BlockEntityEntry<OverheatedBlockEntity> OVERHEATED_BLOCK = REGISTRATE
        .blockEntity("overheated_block", OverheatedBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK)
        .register();

    public static final BlockEntityEntry<PlasmaJetsBlockEntity> PLASMA_JETS = REGISTRATE
        .blockEntity("plasma_jets", PlasmaJetsBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.PLASMA_JETS)
        .renderer(() -> PlasmaJetsRenderer::new)
        .register();

    public static final BlockEntityEntry<DetectorSlidingRailBlockEntity> DETECTOR_SLIDING_RAIL = REGISTRATE
        .blockEntity("detector_sliding_rail", DetectorSlidingRailBlockEntity::new)
        .validBlocks(ModBlocks.DETECTOR_SLIDING_RAIL)
        .register();

    public static final BlockEntityEntry<ActivatorSlidingRailBlockEntity> ACTIVATOR_SLIDING_RAIL = REGISTRATE
        .blockEntity("activator_sliding_rail", ActivatorSlidingRailBlockEntity::new)
        .validBlocks(ModBlocks.ACTIVATOR_SLIDING_RAIL)
        .register();

    public static void register() {
    }
}
