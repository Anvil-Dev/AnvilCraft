package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.preview.StructurePreviewPipRenderer;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.BiConsumer;

/** 为砧库结构预览添加范围框、扫描后处理及轮盘的附加模型。 */
public record StructurePreviewEffects(
    VoxelShape range, float opacity, @Nullable BiConsumer<SubmitNodeCollector, PoseStack> additional
) implements StructurePreviewPipRenderer.Effects {
    @Override
    public void submit(SubmitNodeCollector collector, PoseStack pose) {
        if (this.additional != null) {
            this.additional.accept(collector, pose);
        }
    }

    @Override
    public void render(StructurePreviewPipRenderer.State state) {
        int guiScale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;
        float scale = state.scale() * guiScale;
        if (!this.range.isEmpty()) {
            Matrix4f transform = new Matrix4f().translate(width / 2f, height / 2f, 0)
                .scale(scale, -scale, scale).mul(state.transform());
            drawRangeOverlay(transform, this.range);
        }
        if (this.opacity > 0) {
            drawScanEffect(width, height, this.opacity);
        }
    }

    private static void drawRangeBox(Matrix4f pose, VertexConsumer vertices, VoxelShape shape) {
        // 线宽使用预览纹理的像素坐标，避免主窗口尺寸改变线宽。
        float shrink = (1f - 1f / 256) * 0.99975586f;
        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            var start = pose.transformPosition(new Vector3f((float) x0, (float) y0, (float) z0));
            var end = pose.transformPosition(new Vector3f((float) x1, (float) y1, (float) z1));
            start.mul(shrink);
            end.mul(shrink);
            float dx = end.x - start.x;
            float dy = end.y - start.y;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.0001f) {
                return;
            }
            float offsetX = -dy * 1.25F / length;
            float offsetY = dx * 1.25F / length;
            vertices.addVertex(start.x + offsetX, start.y + offsetY, start.z).setColor(0xFF00FFCC);
            vertices.addVertex(start.x - offsetX, start.y - offsetY, start.z).setColor(0xFF00FFCC);
            vertices.addVertex(end.x - offsetX, end.y - offsetY, end.z).setColor(0xFF00FFCC);
            vertices.addVertex(end.x + offsetX, end.y + offsetY, end.z).setColor(0xFF00FFCC);
        });
    }

    private static void drawRangeOverlay(Matrix4f pose, VoxelShape shape) {
        var format = DefaultVertexFormat.POSITION_COLOR;
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        drawRangeBox(pose, builder, shape);
        var data = builder.buildOrThrow();
        var vertices = format.uploadImmediateVertexBuffer(data.vertexBuffer());
        int count = data.drawState().indexCount();
        data.close();
        var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexBuffer = indices.getBuffer(count);
        var transform = RenderSystem.getDynamicUniforms().writeTransform(
            new Matrix4f(), new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Structure preview range", RenderSystem.outputColorTextureOverride, OptionalInt.empty()
        )) {
            pass.setPipeline(ModRenderPipelines.STRUCTURE_PREVIEW_RANGE);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.setUniform("DynamicTransforms", transform);
            RenderSystem.bindDefaultUniforms(pass);
            pass.drawIndexed(0, 0, count, 1);
        }
    }

    private static void drawScanEffect(int width, int height, float opacity) {
        var effect = ALRPostEffects.getGlitchPostEffect();
        final var texture = effect.process(RenderSystem.outputColorTextureOverride, width, height);
        float u1 = (float) width / effect.getGlitchOutputTarget().width;
        float v1 = (float) height / effect.getGlitchOutputTarget().height;
        var format = DefaultVertexFormat.POSITION_TEX_COLOR;
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        builder.addVertex(0, 0, 100).setUv(0, 0).setColor(-1);
        builder.addVertex(0, height, 100).setUv(0, v1).setColor(-1);
        builder.addVertex(width, height, 100).setUv(u1, v1).setColor(-1);
        builder.addVertex(width, 0, 100).setUv(u1, 0).setColor(-1);
        var data = builder.buildOrThrow();
        var vertices = format.uploadImmediateVertexBuffer(data.vertexBuffer());
        int count = data.drawState().indexCount();
        data.close();
        var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexBuffer = indices.getBuffer(count);
        // 降低强度时保留原图作为底层，让扫描效果与原始方块纹理混合。
        var transform = RenderSystem.getDynamicUniforms().writeTransform(
            new Matrix4f(), new Vector4f(1, 1, 1, opacity / 0.85f), new Vector3f(), new Matrix4f());
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Structure preview scan", RenderSystem.outputColorTextureOverride,
            OptionalInt.empty()
        )) {
            pass.setPipeline(RenderPipelines.GUI_TEXTURED);
            pass.bindTexture("Sampler0", texture, effect.getSampler());
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.setUniform("DynamicTransforms", transform);
            RenderSystem.bindDefaultUniforms(pass);
            pass.drawIndexed(0, 0, count, 1);
        }
    }

}
