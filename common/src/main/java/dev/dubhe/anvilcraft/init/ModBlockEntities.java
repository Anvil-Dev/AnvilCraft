package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;

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

    public static final BlockEntityEntry<TransmissionPoleBlockEntity> REMOTE_TRANSMISSION_POLE = REGISTRATE
        .blockEntity("transmission_pole", TransmissionPoleBlockEntity::createBlockEntity)
        .validBlock(ModBlocks.TRANSMISSION_POLE)
        .register();
    public static final BlockEntityEntry<CrabTrapBlockEntity> CRAB_TRAP = REGISTRATE
        .blockEntity("crab_trap", CrabTrapBlockEntity::createBlockEntity)
        .onRegister(CrabTrapBlockEntity::onBlockEntityRegister)
        .validBlock(ModBlocks.CRAB_TRAP)
        .register();

    public static void register() {
    }
}
