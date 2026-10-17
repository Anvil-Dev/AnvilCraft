package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterInfo;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainHeat;
import dev.dubhe.anvilcraft.util.BlockInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModHeaterInfos {
    public static final HeaterInfo<HeliostatsBlockEntity> HELIOSTATS = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            ModBlockEntities.HELIOSTATS,
            heliostats -> Set.of(heliostats.getIrritatePos(), heliostats.getIrritatePos().above()),
            HeatTierLine.builder()
                .addPoint(4, HeatTier.NORMAL)
                .addPoint(12, HeatTier.HEATED, 4)
                .addPoint(32, HeatTier.REDHOT, 4)
                .addPoint(64, HeatTier.GLOWING, 4)
                .addPoint(HeatTier.INCANDESCENT, 4)
                .build()
        )
    );
    public static final HeaterInfo<BlockInfo> HEATER = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockState(
            ModBlocks.HEATER.get(),
            HeaterBlock.OVERLOAD,
            false,
            heater -> Set.of(heater.pos().above()),
            HeatTierLine.always(HeatTier.HEATED, 2)
        )
    );
    public static final HeaterInfo<BlockInfo> BURNING_HEATER = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockState(
            ModBlocks.BURNING_HEATER.get(),
            BurningHeaterBlock.LEVEL,
            2,
            heater -> Set.of(heater.pos().above()),
            HeatTierLine.always(HeatTier.HEATED, 2)
        )
    );
    /** 数据驱动加热信息缓存：同一个加热定义只注册一次。 */
    private static final Map<MineralFountainHeat, HeaterInfo<MineralFountainBlockEntity>> MINERAL_FOUNTAIN_HEATS =
        new HashMap<>();

    /**
     * 取某个加热定义对应的供热信息；由矿物涌泉配方在匹配时登记。
     */
    public static HeaterInfo<MineralFountainBlockEntity> mineralFountain(MineralFountainHeat heat) {
        return MINERAL_FOUNTAIN_HEATS.computeIfAbsent(heat, key -> HeatRecorder.registerProducerInfo(
            HeaterInfo.blockEntity(
                ModBlockEntities.MINERAL_FOUNTAIN,
                fountain -> Set.of(fountain.getBlockPos().above()),
                key.toLine(),
                fountain -> key.count()
            )
        ));
    }

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
