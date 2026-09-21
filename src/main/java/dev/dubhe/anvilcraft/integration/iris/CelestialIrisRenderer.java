package dev.dubhe.anvilcraft.integration.iris;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.GlStateBackup;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/** Replays celestial emission and atmosphere against scene depth after the shader pack's final color pass. */
public final class CelestialIrisRenderer {
    private static final List<Runnable> DRAWS = new ArrayList<>();
    private static final List<Runnable> ATMOSPHERES = new ArrayList<>();
    private static final SequencedMap<RenderType, ByteBufferBuilder> STORAGE = new LinkedHashMap<>();
    private static @Nullable ByteBufferBuilder shared;
    private static @Nullable MultiBufferSource.BufferSource buffers;
    private static @Nullable FrameState frameState;
    private static boolean collecting;
    private static boolean rendering;
    private static boolean renderingAtmospheres;
    private static boolean failed;

    private CelestialIrisRenderer() {
    }

    public static void beginFrame() {
        DRAWS.clear();
        ATMOSPHERES.clear();
        frameState = null;
        collecting = !failed;
    }

    public static boolean isRendering() {
        return rendering;
    }

    public static boolean defer(PoseStack pose, BiConsumer<PoseStack, MultiBufferSource> draw) {
        return defer(pose, draw, DRAWS);
    }

    private static boolean defer(PoseStack pose, BiConsumer<PoseStack, MultiBufferSource> draw, List<Runnable> queue) {
        boolean lateAtmosphere = rendering && !renderingAtmospheres && queue == ATMOSPHERES;
        if ((!collecting && !lateAtmosphere) || failed) return false;
        // First-person items have their own projection and compressed depth range.
        if (HandRenderer.INSTANCE.isActive()) return false;
        // Shadow passes use light-space poses; these must never enter the camera's draw queue.
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return true;
        if (frameState == null) frameState = FrameState.capture();
        PoseStack copy = new PoseStack();
        copy.mulPose(pose.last().pose());
        copy.last().normal().set(pose.last().normal());
        queue.add(() -> {
            MultiBufferSource.BufferSource target = buffers;
            if (target != null) draw.accept(copy, target);
        });
        return true;
    }

    public static boolean deferAtmosphere(PoseStack pose, BiConsumer<PoseStack, MultiBufferSource> draw) {
        return defer(pose, draw, ATMOSPHERES);
    }

    public static void render(RenderType surface, RenderType corona, RenderType atmosphere) {
        collecting = false;
        FrameState captured = frameState;
        if ((DRAWS.isEmpty() && ATMOSPHERES.isEmpty()) || captured == null) return;
        try (RenderScope ignored = new RenderScope()) {
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            main.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            captured.apply();
            rendering = true;
            if (buffers == null) {
                shared = new ByteBufferBuilder(1536);
                buffers = new CelestialBufferSource(shared, surface, corona, atmosphere);
            }
            for (Runnable draw : DRAWS) draw.run();
            buffers.endLastBatch();
            // All stellar surfaces must populate depth before any star's transparent halo is drawn.
            buffers.endBatch(surface);
            buffers.endBatch();
            // Atmospheres own per-planet uniforms and must not flush before stellar surfaces populate depth.
            renderingAtmospheres = true;
            for (Runnable atmosphereDraw : ATMOSPHERES) atmosphereDraw.run();
            buffers.endBatch();
            if (buffers instanceof CelestialBufferSource celestialBuffers) celestialBuffers.finishFrame();
        } catch (RuntimeException exception) {
            failed = true;
            releaseBuffers();
            AnvilCraft.LOGGER.warn("Celestial shader composition failed; using compatibility rendering until reload.", exception);
        } finally {
            rendering = false;
            renderingAtmospheres = false;
            DRAWS.clear();
            ATMOSPHERES.clear();
            frameState = null;
        }
    }

    public static void reset() {
        collecting = false;
        rendering = false;
        renderingAtmospheres = false;
        failed = false;
        DRAWS.clear();
        ATMOSPHERES.clear();
        frameState = null;
        releaseBuffers();
    }

    private static void releaseBuffers() {
        buffers = null;
        STORAGE.values().forEach(ByteBufferBuilder::close);
        STORAGE.clear();
        if (shared != null) shared.close();
        shared = null;
    }

    private static final class CelestialBufferSource extends MultiBufferSource.BufferSource {
        private final Map<RenderType, RenderType> types = new IdentityHashMap<>();

        private CelestialBufferSource(ByteBufferBuilder shared, RenderType surface, RenderType corona, RenderType atmosphere) {
            super(shared, STORAGE);
            this.fixedBuffers.put(this.mainTarget(surface), new ByteBufferBuilder(1536));
            this.fixedBuffers.put(this.mainTarget(corona), new ByteBufferBuilder(8192));
            this.fixedBuffers.put(this.mainTarget(atmosphere), new ByteBufferBuilder(8192));
        }

        private RenderType mainTarget(RenderType type) {
            return this.types.computeIfAbsent(type, original -> new MainTargetRenderType(original,
                () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(true)));
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return super.getBuffer(this.mainTarget(type));
        }

        @Override
        public void endBatch(RenderType type) {
            super.endBatch(this.types.getOrDefault(type, type));
        }

        private void finishFrame() {
            // Skins, baked planet textures and per-planet shader states must not accumulate between frames.
            this.types.entrySet().removeIf(entry -> !this.fixedBuffers.containsKey(entry.getValue()));
        }
    }

    private static final class MainTargetRenderType extends RenderType {
        private MainTargetRenderType(RenderType original, Runnable bindTarget) {
            super("anvilcraft:celestial_composition", original.format(), original.mode(), original.bufferSize(),
                original.affectsCrumbling(), original.sortOnUpload(), () -> {
                    // COLOR_DEPTH_WRITE assumes this default; Iris's final pass leaves depth writes disabled.
                    RenderSystem.depthMask(true);
                    original.setupRenderState();
                    // Some vanilla translucent types select a Fabulous target whose composition has already finished.
                    bindTarget.run();
                }, original::clearRenderState);
        }
    }

    private record FrameState(
        Matrix4f modelView, Matrix4f projection, VertexSorting sorting,
        float fogStart, float fogEnd, FogShape fogShape, float[] fogColor, float[] color
    ) {
        private static FrameState capture() {
            return new FrameState(new Matrix4f(RenderSystem.getModelViewMatrix()), new Matrix4f(RenderSystem.getProjectionMatrix()),
                RenderSystem.getVertexSorting(), RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(),
                RenderSystem.getShaderFogShape(), RenderSystem.getShaderFogColor().clone(), RenderSystem.getShaderColor().clone());
        }

        private void apply() {
            RenderSystem.getModelViewStack().set(this.modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(this.projection, this.sorting);
            RenderSystem.setShaderFogStart(this.fogStart);
            RenderSystem.setShaderFogEnd(this.fogEnd);
            RenderSystem.setShaderFogShape(this.fogShape);
            RenderSystem.setShaderFogColor(this.fogColor[0], this.fogColor[1], this.fogColor[2], this.fogColor[3]);
            RenderSystem.setShaderColor(this.color[0], this.color[1], this.color[2], this.color[3]);
        }
    }

    private static final class RenderScope implements AutoCloseable {
        private final GlStateBackup state = new GlStateBackup();
        private final FrameState previous = FrameState.capture();
        private final @Nullable ShaderInstance shader = RenderSystem.getShader();
        private final int program = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        private final int drawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        private final int activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        private final int[] textures = new int[3];
        private final int[] bindings = new int[3];
        private final int[] viewport = new int[4];
        private final boolean renderingLevel = ImmediateState.isRenderingLevel;
        private final boolean extendedFormat = ImmediateState.renderWithExtendedVertexFormat;
        private final boolean skipExtension = ImmediateState.skipExtension.get();

        private RenderScope() {
            RenderSystem.backupGlState(this.state);
            // Iris also changes GL directly, so query the real state at this boundary.
            this.state.blendEnabled = GL11C.glIsEnabled(GL11C.GL_BLEND);
            this.state.blendSrcRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
            this.state.blendDestRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
            this.state.blendSrcAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
            this.state.blendDestAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
            this.state.depthEnabled = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
            this.state.depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
            this.state.depthFunc = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC);
            this.state.cullEnabled = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
            this.state.scissorEnabled = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer mask = stack.malloc(4);
                GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, mask);
                this.state.colorMaskRed = mask.get(0) != 0;
                this.state.colorMaskGreen = mask.get(1) != 0;
                this.state.colorMaskBlue = mask.get(2) != 0;
                this.state.colorMaskAlpha = mask.get(3) != 0;
            }
            RenderSystem.restoreGlState(this.state);
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, this.viewport);
            for (int unit = 0; unit < this.textures.length; unit++) {
                this.textures[unit] = RenderSystem.getShaderTexture(unit);
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit);
                this.bindings[unit] = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
            }
            RenderSystem.activeTexture(this.activeTexture);
            ImmediateState.isRenderingLevel = false;
            ImmediateState.renderWithExtendedVertexFormat = false;
            ImmediateState.skipExtension.set(true);
        }

        @Override
        public void close() {
            ImmediateState.isRenderingLevel = this.renderingLevel;
            ImmediateState.renderWithExtendedVertexFormat = this.extendedFormat;
            ImmediateState.skipExtension.set(this.skipExtension);
            this.previous.apply();
            RenderSystem.setShader(() -> this.shader);
            for (int unit = 0; unit < this.textures.length; unit++) {
                RenderSystem.setShaderTexture(unit, this.textures[unit]);
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit);
                GlStateManager._bindTexture(this.bindings[unit]);
            }
            RenderSystem.activeTexture(this.activeTexture);
            GlStateManager._glUseProgram(this.program);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.readFramebuffer);
            RenderSystem.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
            RenderSystem.restoreGlState(this.state);
        }
    }
}
