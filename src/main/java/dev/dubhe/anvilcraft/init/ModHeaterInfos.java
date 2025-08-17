package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterInfo;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.Set;

public class ModHeaterInfos {
    public static final HeaterInfo<HeliostatsBlockEntity> HELIOSTATS = HeatRecorder.registerProducerInfo(HeaterInfo.blockEntity(
        ModBlockEntities.HELIOSTATS,
        heliostats -> Set.of(heliostats.getIrritatePos(), heliostats.getIrritatePos().above()),
        HeatTierLine.builder()
            .addPoint(4, HeatTier.NORMAL)
            .addPoint(12, HeatTier.HEATED, 4)
            .addPoint(32, HeatTier.REDHOT, 4)
            .addPoint(64, HeatTier.GLOWING, 4)
            .addPoint(HeatTier.INCANDESCENT, 4)
            .build())
    );
    public static final HeaterInfo<HeaterBlockEntity> HEATER = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            (level, pos) -> level.getBlockEntity(pos, ModBlockEntities.HEATER.get())
                .filter(heater -> !heater.getBlockState().getValue(HeaterBlock.OVERLOAD)),
            heater -> Set.of(heater.getBlockPos().above()),
            HeatTierLine.always(HeatTier.HEATED, 2))
    );
    public static final HeaterInfo<MineralFountainBlockEntity> LAVA_MINERAL_FOUNTAIN = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            (level, pos) -> level.getBlockEntity(pos, ModBlockEntities.MINERAL_FOUNTAIN.get())
                .filter(mineralFountain -> mineralFountain.getAroundBlock().is(Blocks.LAVA)),
            mineralFountain -> Set.of(mineralFountain.getBlockPos().above()),
            HeatTierLine.always(HeatTier.REDHOT, 20))
    );
    @SuppressWarnings("OptionalOfNullableMisuse")
    public static final HeaterInfo<BaseLaserBlockEntity> LASER_EMITTER = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            (level, pos) -> Util.castSafely(level.getBlockEntity(pos), BaseLaserBlockEntity.class),
            laserEmitter -> Optional.ofNullable(laserEmitter.getIrradiateBlockPos())
                .map(Set::of)
                .orElse(Set.of()),
            HeatTierLine.builder()
                .addPoint(1, HeatTier.NORMAL)
                .addPoint(4, HeatTier.HEATED, 2)
                .addPoint(16, HeatTier.REDHOT, 2)
                .addPoint(64, HeatTier.GLOWING, 2)
                .addPoint(HeatTier.INCANDESCENT, 2)
                .build(),
            BaseLaserBlockEntity::getLaserLevel)
    );
    public static final HeaterInfo<PlasmaJetsBlockEntity> NO_MAGNET_PLASMA_JETS = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            ModBlockEntities.PLASMA_JETS,
            plasmaJets -> plasmaJets.getHeatingPoses().getFirst(),
            HeatTierLine.always(HeatTier.GLOWING, 2))
    );
    public static final HeaterInfo<PlasmaJetsBlockEntity> MAGNET_PLASMA_JETS = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            ModBlockEntities.PLASMA_JETS,
            plasmaJets -> plasmaJets.getHeatingPoses().getSecond(),
            HeatTierLine.always(HeatTier.GLOWING, 20))
    );
}
