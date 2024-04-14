package dev.dubhe.anvilcraft.api.power;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 电力元件
 */
@SuppressWarnings("unused")
public interface IPowerComponent {
    /**
     * @return 元件位置
     */
    @NotNull
    BlockPos getPos();

    /**
     * 设置电网
     *
     * @param grid 电网
     */
    void setGrid(@Nullable PowerGrid grid);

    /**
     * 获取电网
     *
     * @return 电网
     */
    @Nullable
    PowerGrid getGrid();

    /**
     * @return 元件类型
     */
    @NotNull
    PowerComponentType getComponentType();
}
