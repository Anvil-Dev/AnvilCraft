package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
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
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HasMobBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HeliostatsRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LaserRenderer;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModBlockEntities {
    public static final BlockEntityEntry<AutoCrafterBlockEntity> AUTO_CRAFTER = REGISTRATE
        .blockEntity("auto_crafter", AutoCrafterBlockEntity::createBlockEntity)
        .onRegister(AutoCrafterBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.AUTO_CRAFTER)
        .register();

    public static final BlockEntityEntry<ItemCollectorBlockEntity> ITEM_COLLECTOR = REGISTRATE
            .blockEntity("item_collector", ItemCollectorBlockEntity::createBlockEntity)
            .onRegister(ItemCollectorBlockEntity::onBlockEntityRegister)
            .validBlock(ModBlocks.ITEM_COLLECTOR)
            .register();

    public static final BlockEntityEntry<ChuteBlockEntity> CHUTE = REGISTRATE
        .blockEntity("chute", ChuteBlockEntity::createBlockEntity)
        .onRegister(ChuteBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CHUTE)
        .register();

    public static final BlockEntityEntry<SimpleChuteBlockEntity> SIMPLE_CHUTE = REGISTRATE
        .blockEntity("simple_chute", SimpleChuteBlockEntity::createBlockEntity)
        .onRegister(SimpleChuteBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.SIMPLE_CHUTE)
        .register();

    public static final BlockEntityEntry<CrabTrapBlockEntity> CRAB_TRAP = REGISTRATE
        .blockEntity("crab_trap", CrabTrapBlockEntity::createBlockEntity)
        .onRegister(CrabTrapBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CRAB_TRAP)
        .register();

    public static final BlockEntityEntry<CorruptedBeaconBlockEntity> CORRUPTED_BEACON = REGISTRATE
        .blockEntity("corrupted_beacon", CorruptedBeaconBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.CORRUPTED_BEACON)
        .renderer(() -> CorruptedBeaconRenderer::new)
        .register();

    public static final BlockEntityEntry<CreativeGeneratorBlockEntity> CREATIVE_GENERATOR = REGISTRATE
        .blockEntity("creative_generator", CreativeGeneratorBlockEntity::createBlockEntity)
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
        .register();

    public static final BlockEntityEntry<MobAmberBlockEntity> MOB_AMBER_BLOCK = REGISTRATE
        .blockEntity("mob_amber_block", MobAmberBlockEntity::createBlockEntity)
        .renderer(() -> HasMobBlockRenderer::new)
        .validBlock(ModBlocks.MOB_AMBER_BLOCK)
        .register();

    public static final BlockEntityEntry<ResentfulAmberBlock> RESENTFUL_AMBER_BLOCK = REGISTRATE
        .blockEntity("resentful_amber_block", ResentfulAmberBlock::createBlockEntity)
        .renderer(() -> HasMobBlockRenderer::new)
        .validBlock(ModBlocks.RESENTFUL_AMBER_BLOCK)
        .register();

    public static final BlockEntityEntry<PowerConverterBlockEntity> POWER_CONVERTER = REGISTRATE
        .blockEntity("power_converter", PowerConverterBlockEntity::createBlockEntity)
        .validBlocks(ModBlocks.POWER_CONVERTER_SMALL, ModBlocks.POWER_CONVERTER_MIDDLE, ModBlocks.POWER_CONVERTER_BIG)
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
            .onRegister(ChargerBlockEntity::onBlockEntityRegister)
            .register();

    public static final BlockEntityEntry<ActiveSilencerBlockEntity> ACTIVE_SILENCER = REGISTRATE
            .blockEntity("active_silencer", ActiveSilencerBlockEntity::new)
            .validBlocks(ModBlocks.ACTIVE_SILENCER)
            .register();

    public static final BlockEntityEntry<RubyPrismBlockEntity> RUBY_PRISM = REGISTRATE
        .blockEntity("ruby_prism", RubyPrismBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.RUBY_PRISM)
        .renderer(() -> LaserRenderer::new)
        .register();
    public static final BlockEntityEntry<RubyLaserBlockEntity> RUBY_LASER = REGISTRATE
        .blockEntity("ruby_laser", RubyLaserBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.RUBY_LASER)
        .renderer(() -> LaserRenderer::new)
        .register();

    public static final BlockEntityEntry<ThermoelectricConverterBlockEntity> THERMOELECTRIC_CONVERTER = REGISTRATE
            .blockEntity("thermoelectric_converter", ThermoelectricConverterBlockEntity::new)
            .validBlocks(ModBlocks.THERMOELECTRIC_CONVERTER)
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

    public static void register() {
    }
}
