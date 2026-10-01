package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.List;

/// 腐化信标 in-world 渲染状态。extract 阶段捕获光束是否点亮及其顶端高度。
@Getter
@Setter
public class CorruptedBeaconRenderState extends BlockEntityRenderState {
    private List<BakedQuad> glassQuads = List.of();

    /// 是否点亮（渲染光束）
    private boolean lit;
    /// 光束的局部高度（世界顶端 Y - 方块 Y - BEAM_BASE_Y），> 0.5 时才绘制
    private float beamHeight;
}
