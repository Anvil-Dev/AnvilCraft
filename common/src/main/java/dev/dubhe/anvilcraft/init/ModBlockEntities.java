package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LoadMonitorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MobAmberBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RemoteTransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ResentfulAmberBlock;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.HasMobBlockRenderer;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModBlockEntities {
    public static final BlockEntityEntry<AutoCrafterBlockEntity> AUTO_CRAFTER = REGISTRATE
        .blockEntity("auto_crafter", AutoCrafterBlockEntity::createBlockEntity)
        .onRegister(AutoCrafterBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.AUTO_CRAFTER)
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

    public static final BlockEntityEntry<OverseerBlockEntity> OVERSEER = REGISTRATE
        .blockEntity("overseer", OverseerBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.OVERSEER_BLOCK)
        .register();

    public static void register() {
    }
}
