package dev.dubhe.anvilcraft.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/// 描述非标准炼药锅的内部空间与承载方块
public interface ICauldronGeometry {
    /// 获取锅内可容纳实体的区域
    AABB getCauldronInnerArea(BlockPos pos, BlockState state);

    /// 获取锅底下方用于加热的方块位置
    List<BlockPos> getCauldronBottomPositions(BlockPos pos, BlockState state);
}
