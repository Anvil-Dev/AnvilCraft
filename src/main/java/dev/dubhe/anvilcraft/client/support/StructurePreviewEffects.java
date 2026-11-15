package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.anvilcraft.lib.v2.rendering.preview.level.StructurePreviewRenderer;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.function.BiConsumer;

/** 在共享渲染器上叠加扫描范围，最终纹理按指定强度混合扫描效果。 */
public record StructurePreviewEffects(VoxelShape range, int width, int height,
    @Nullable BiConsumer<MultiBufferSource.BufferSource, PoseStack> additional) implements StructurePreviewRenderer.Effects {
    @Override
    public void submit(MultiBufferSource.BufferSource buffers) {
        if (this.additional != null) {
            this.additional.accept(buffers, new PoseStack());
        }
    }

    @Override
    public void render() {
        if (this.range.isEmpty()) {
            return;
        }
        var transform = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        try (PreviewRenderState state = new PreviewRenderState()) {
            RenderSystem.getModelViewStack().identity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            this.range.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
                var first = transform.transformProject(new Vector3f((float) x0, (float) y0, (float) z0));
                var last = transform.transformProject(new Vector3f((float) x1, (float) y1, (float) z1));
                float dx = (last.x - first.x) * this.width;
                float dy = (last.y - first.y) * this.height;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length < 0.0001f) {
                    return;
                }
                float ox = -dy / length * 2.5f / this.width;
                float oy = dx / length * 2.5f / this.height;
                vertices.addVertex(first.x + ox, first.y + oy, 0).setColor(0xFF00FFCC);
                vertices.addVertex(first.x - ox, first.y - oy, 0).setColor(0xFF00FFCC);
                vertices.addVertex(last.x - ox, last.y - oy, 0).setColor(0xFF00FFCC);
                vertices.addVertex(last.x + ox, last.y + oy, 0).setColor(0xFF00FFCC);
            });
            var data = vertices.build();
            if (data != null) {
                BufferUploader.drawWithShader(data);
            }
        }
    }

    public static void blit(GuiGraphics graphics, RenderTarget target, int x0, int y0, int x1, int y1, float opacity) {
        try (PreviewRenderState state = new PreviewRenderState()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            var matrix = graphics.pose().last().pose();
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertices.addVertex(matrix, x0, y0, 0).setUv(0, 1);
            vertices.addVertex(matrix, x0, y1, 0).setUv(0, 0);
            vertices.addVertex(matrix, x1, y1, 0).setUv(1, 0);
            vertices.addVertex(matrix, x1, y0, 0).setUv(1, 1);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
            if (opacity > 0) {
                var minecraft = Minecraft.getInstance();
                final float guiScale = (float) minecraft.getWindow().getGuiScale();
                final var first = matrix.transformPosition(new Vector3f(x0, y0, 0));
                final var last = matrix.transformPosition(new Vector3f(x1, y1, 0));
                var shader = ModShaders.getScanPreviewShader();
                shader.setSampler("DiffuseSampler", target);
                shader.safeGetUniform("ProjMat").set(new Matrix4f().setOrtho(0, minecraft.getWindow().getWidth(),
                    0, minecraft.getWindow().getHeight(), -1, 1));
                shader.safeGetUniform("InSize").set((float) target.width, (float) target.height);
                shader.safeGetUniform("OutPos").set(first.x * guiScale,
                    (minecraft.getWindow().getGuiScaledHeight() - last.y) * guiScale);
                shader.safeGetUniform("OutSize").set((last.x - first.x) * guiScale, (last.y - first.y) * guiScale);
                shader.safeGetUniform("GameTime").set((System.currentTimeMillis() % 100000) / 1000f);
                shader.safeGetUniform("ScanOpacity").set(opacity);
                shader.apply();
                try {
                    var quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                    quad.addVertex(0, 0, 0);
                    quad.addVertex(target.width, 0, 0);
                    quad.addVertex(target.width, target.height, 0);
                    quad.addVertex(0, target.height, 0);
                    BufferUploader.draw(quad.buildOrThrow());
                } finally {
                    shader.clear();
                }
            }
        }
    }
}
