package dev.dubhe.anvilcraft.client.renderer.debug;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;

/// 锻星砧渲染包围盒调试渲染器，由 F3 调试选项 `anvilcraft:cfa_render_bounds` 开关。
///
/// 按原版调试渲染器的做法：在提取阶段迭代可见方块实体，几何通过 {@link Gizmos} 提交，
/// 由原版每帧收集器统一绘制。
public class CfaRenderBoundsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    /// 调试条目 ID，用于注册 F3 调试选项并查询开关状态。
    public static final Identifier LOCATION = AnvilCraft.of("cfa_render_bounds");

    private static final float BOUNDS_WIDTH = 4.0f;
    /// 颜色取自包围盒哈希，只保留其 RGB 分量，alpha 强制不透明以保证轮廓可见。
    private static final int BOUNDS_ALPHA = 0xFF000000;

    @Override
    public void emitGizmos(
        double camX,
        double camY,
        double camZ,
        DebugValueAccess debugValues,
        Frustum frustum,
        float partialTicks
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.debugEntries.isCurrentlyEnabled(CfaRenderBoundsDebugRenderer.LOCATION)) {
            return;
        }
        BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
        minecraft.levelRenderer.iterateVisibleBlockEntities(be -> {
            if (!(be instanceof CelestialForgingAnvilBlockEntity cfa)) return;
            BlockEntityRenderer<CelestialForgingAnvilBlockEntity, ?> renderer = dispatcher.getRenderer(cfa);
            if (!(renderer instanceof CFARenderer cfaRenderer)) return;
            AABB bounds = cfaRenderer.getOcclusionBoundingBox(cfa, partialTicks);
            Gizmos.cuboid(
                bounds,
                GizmoStyle.stroke(
                    CfaRenderBoundsDebugRenderer.BOUNDS_ALPHA | bounds.hashCode(),
                    CfaRenderBoundsDebugRenderer.BOUNDS_WIDTH
                )
            );
        });
    }
}
