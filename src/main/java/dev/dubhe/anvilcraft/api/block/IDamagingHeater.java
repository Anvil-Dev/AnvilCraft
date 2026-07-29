package dev.dubhe.anvilcraft.api.block;

import net.minecraft.world.level.block.state.BlockState;

/// 会对站在其上或位于其上方锅内的实体造成伤害的加热器
public interface IDamagingHeater {
    /// 该状态下加热器是否正在工作
    boolean isActive(BlockState state);
}
