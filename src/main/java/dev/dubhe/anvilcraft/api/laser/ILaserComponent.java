package dev.dubhe.anvilcraft.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** 相等性表示组件配置相同；重新合并时，相同配置保留当前光束实例的运行状态。 */
public interface ILaserComponent {
    default void onEmitPre(ILaserComponentOwner owner) {
    }

    /** 返回 false 时阻止后续组件对此方块执行行为。 */
    default boolean onHitBlock(ILaserComponentOwner owner, Level level, BlockPos blockPos) {
        return true;
    }

    default void onEmissionStopped(ILaserComponentOwner owner) {
    }
}
