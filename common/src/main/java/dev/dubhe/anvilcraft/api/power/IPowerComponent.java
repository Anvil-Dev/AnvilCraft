package dev.dubhe.anvilcraft.api.power;

import net.minecraft.core.BlockPos;

/**
 * 电力元件
 */
@SuppressWarnings("unused")
public interface IPowerComponent {
    /**
     * @return 元件位置
     */
    BlockPos getPos();

    /**
     * 设置电网
     *
     * @param grid 电网
     */
    void setGrid(PowerGrid grid);

    /**
     * @return 元件类型
     */
    PowerComponentType getType();
}
