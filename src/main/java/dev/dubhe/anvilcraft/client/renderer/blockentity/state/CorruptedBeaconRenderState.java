package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/// 腐化信标 in-world 渲染状态。extract 阶段捕获光束是否点亮及其顶端高度。
@Getter
@Setter
public class CorruptedBeaconRenderState extends BlockEntityRenderState {
    /// 是否点亮（渲染光束）
    private boolean lit;
    /// 光束的局部高度（世界顶端 Y - 方块 Y - BEAM_BASE_Y），> 0 时才绘制
    private float beamHeight;
}
