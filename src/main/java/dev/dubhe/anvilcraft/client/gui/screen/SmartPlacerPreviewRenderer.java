package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.StructurePipRenderer;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

/** 扫描效果需要同时处理预览背景和结构，保持源版本的合成顺序。 */
public final class SmartPlacerPreviewRenderer extends PictureInPictureRenderer<SmartPlacerPreviewRenderer.State> {
    private static final ThreadLocal<Boolean> SCAN_SCOPE = ThreadLocal.withInitial(() -> false);
    private final StructureRenderer structures;

    public SmartPlacerPreviewRenderer(MultiBufferSource.BufferSource buffer) {
        super(buffer);
        this.structures = new StructureRenderer(buffer);
    }

    public static boolean isRenderingScan() {
        return SCAN_SCOPE.get();
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        if (state.structure().glitched()) this.drawBackground(state);
        this.structures.draw(state.structure(), pose);
    }

    private void drawBackground(State state) {
        Minecraft client = Minecraft.getInstance();
        int guiScale = client.gameRenderer.getGameRenderState().windowRenderState.guiScale;
        float width = (state.x1() - state.x0()) * guiScale;
        float height = (state.y1() - state.y0()) * guiScale;
        var format = DefaultVertexFormat.POSITION_TEX_COLOR;
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        float u0 = 136.0F / 256;
        float v0 = 18.0F / 256;
        float u1 = 248.0F / 256;
        float v1 = 106.0F / 256;
        builder.addVertex(0, 0, 0).setUv(u0, v0).setColor(-1);
        builder.addVertex(0, height, 0).setUv(u0, v1).setColor(-1);
        builder.addVertex(width, height, 0).setUv(u1, v1).setColor(-1);
        builder.addVertex(width, 0, 0).setUv(u1, v0).setColor(-1);
        var data = builder.buildOrThrow();
        var vertices = format.uploadImmediateVertexBuffer(data.vertexBuffer());
        int count = data.drawState().indexCount();
        data.close();
        var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexBuffer = indices.getBuffer(count);
        var transform = RenderSystem.getDynamicUniforms().writeTransform(
            new Matrix4f(), new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
        var texture = client.getTextureManager().getTexture(state.background());
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Smart placer preview background", RenderSystem.outputColorTextureOverride, OptionalInt.empty()
        )) {
            pass.setPipeline(RenderPipelines.GUI_TEXTURED);
            pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.setUniform("DynamicTransforms", transform);
            RenderSystem.bindDefaultUniforms(pass);
            pass.drawIndexed(0, 0, count, 1);
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 4F;
    }

    @Override
    protected String getTextureLabel() {
        return "anvilcraft_smart_placer_preview";
    }

    @Override
    public void close() {
        this.structures.close();
        super.close();
    }

    public static final class StructureRenderer extends StructurePipRenderer {
        private StructureRenderer(MultiBufferSource.BufferSource buffer) {
            super(buffer);
        }

        private void draw(StructurePipRenderingState state, PoseStack pose) {
            boolean previous = SCAN_SCOPE.get();
            SCAN_SCOPE.set(true);
            try {
                super.renderToTexture(state, pose);
            } finally {
                SCAN_SCOPE.set(previous);
            }
        }
    }

    public record State(StructurePipRenderingState structure, Identifier background) implements PictureInPictureRenderState {
        public State(StructurePipRenderingState structure) {
            this(structure, SharedTextures.bg("machine", "smart_block_placer"));
        }

        @Override
        public int x0() {
            return this.structure.x0();
        }

        @Override
        public int x1() {
            return this.structure.x1();
        }

        @Override
        public int y0() {
            return this.structure.y0();
        }

        @Override
        public int y1() {
            return this.structure.y1();
        }

        @Override
        public float scale() {
            return this.structure.scale();
        }

        @Override
        public Matrix3x2f pose() {
            return this.structure.pose();
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return this.structure.scissorArea();
        }

        @Override
        public @Nullable ScreenRectangle bounds() {
            return this.structure.bounds();
        }
    }
}
