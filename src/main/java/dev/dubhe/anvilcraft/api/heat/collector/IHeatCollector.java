package dev.dubhe.anvilcraft.api.heat.collector;

import net.minecraft.core.BlockPos;

/**
 * 热能收集器接口，用于统一处理热能收集逻辑。
 */
public interface IHeatCollector {
    BlockPos getCollectorPos();

    boolean isCollectorWorking();

    void setCollectorWorking(boolean working);

    int inputHeat(int amount);

    int getCollectorRange();
}
