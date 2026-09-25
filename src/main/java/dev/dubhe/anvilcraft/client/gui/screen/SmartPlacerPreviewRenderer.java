package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.StructurePipRenderer;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

/** 扫描效果需要同时处理预览背景和结构，保持源版本的合成顺序。 */
public final class SmartPlacerPreviewRenderer extends PictureInPictureRenderer<SmartPlacerPreviewRenderer.State> {
    private static final ThreadLocal<Boolean> SCAN_SCOPE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<RangeBox> RANGE_BOX = new ThreadLocal<>();
    private final StructureRenderer structures;

    private record RangeBox(Matrix4f pose, int originX, int originY) {
    }

    public SmartPlacerPreviewRenderer(MultiBufferSource.BufferSource buffer) {
        super(buffer);
        this.structures = new StructureRenderer(buffer);
    }

    public static boolean isRenderingScan() {
        return SCAN_SCOPE.get();
    }

    public static void captureRangeBox(Matrix4f pose, int originX, int originY) {
        RANGE_BOX.set(new RangeBox(pose, originX, originY));
    }

    private static void drawRangeBox(Matrix4f pose, VertexConsumer vertices, int originX, int originY) {
        // Vanilla line shaders use the main window size, so extrude in this preview's pixel coordinates instead.
        float shrink = (1.0F - 1.0F / 256.0F) * 0.99975586F;
        Shapes.create(0, 0, 0, 5, 5, 5).forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            var start = pose.transformPosition(new Vector3f((float) x0, (float) y0, (float) z0));
            var end = pose.transformPosition(new Vector3f((float) x1, (float) y1, (float) z1));
            start.mul(shrink).add(-originX * (1.0F - shrink), -originY * (1.0F - shrink), 0);
            end.mul(shrink).add(-originX * (1.0F - shrink), -originY * (1.0F - shrink), 0);
            float dx = end.x - start.x;
            float dy = end.y - start.y;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.0001F) return;
            float offsetX = -dy * 1.25F / length;
            float offsetY = dx * 1.25F / length;
            vertices.addVertex(start.x + offsetX, start.y + offsetY, start.z).setColor(0xFF00FFCC);
            vertices.addVertex(start.x - offsetX, start.y - offsetY, start.z).setColor(0xFF00FFCC);
            vertices.addVertex(end.x - offsetX, end.y - offsetY, end.z).setColor(0xFF00FFCC);
            vertices.addVertex(end.x + offsetX, end.y + offsetY, end.z).setColor(0xFF00FFCC);
        });
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        RangeBox previous = RANGE_BOX.get();
        boolean previousScan = SCAN_SCOPE.get();
        RANGE_BOX.remove();
        SCAN_SCOPE.set(true);
        try {
            if (state.structure().glitched()) this.drawBackground(state);
            this.structures.draw(state.structure(), pose);
            RangeBox range = RANGE_BOX.get();
            if (range != null) this.drawRangeOverlay(range);
            if (state.structure().glitched()) {
                int scale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
                this.structures.applyGlitchEffect((state.x1() - state.x0()) * scale, (state.y1() - state.y0()) * scale);
            }
        } finally {
            SCAN_SCOPE.set(previousScan);
            if (previous == null) RANGE_BOX.remove();
            else RANGE_BOX.set(previous);
        }
    }

    private void drawRangeOverlay(RangeBox range) {
        var format = DefaultVertexFormat.POSITION_COLOR;
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        drawRangeBox(range.pose(), builder, range.originX(), range.originY());
        var data = builder.buildOrThrow();
        var vertices = format.uploadImmediateVertexBuffer(data.vertexBuffer());
        int count = data.drawState().indexCount();
        data.close();
        var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexBuffer = indices.getBuffer(count);
        var transform = RenderSystem.getDynamicUniforms().writeTransform(
            new Matrix4f(), new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Smart placer range overlay", RenderSystem.outputColorTextureOverride, OptionalInt.empty()
        )) {
            pass.setPipeline(ModRenderPipelines.SMART_PLACER_RANGE);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.setUniform("DynamicTransforms", transform);
            RenderSystem.bindDefaultUniforms(pass);
            pass.drawIndexed(0, 0, count, 1);
        }
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
        private boolean deferGlitch;

        private StructureRenderer(MultiBufferSource.BufferSource buffer) {
            super(buffer);
        }

        private void draw(StructurePipRenderingState state, PoseStack pose) {
            boolean previous = SCAN_SCOPE.get();
            boolean previousDefer = this.deferGlitch;
            this.deferGlitch = true;
            SCAN_SCOPE.set(true);
            try {
                super.renderToTexture(state, pose);
            } finally {
                this.deferGlitch = previousDefer;
                SCAN_SCOPE.set(previous);
            }
        }

        @Override
        public void applyGlitchEffect(int width, int height) {
            if (!this.deferGlitch) super.applyGlitchEffect(width, height);
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
