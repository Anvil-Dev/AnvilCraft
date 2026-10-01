package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.OverworldSkyMode;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Vector;
import dev.dubhe.anvilcraft.worldgen.OverworldSkyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class OverworldSkyRenderer {
    private static final Identifier MOON = AnvilCraft.of("textures/block/celestial_body/planet_atmosphereless.png");
    private static final Identifier SUN = AnvilCraft.of("block/celestial_body/star");
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(AnvilCraft.of("pipeline/overworld_sky"))
        .withVertexShader(AnvilCraft.of("core/mun/overworld_sky"))
        .withFragmentShader(AnvilCraft.of("core/mun/overworld_sky"))
        .withUniform("OverworldSky", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0").withSampler("Sampler1").withSampler("Sampler2")
        .withColorTargetState(new ColorTargetState(new BlendFunction(
            SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)))
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withDepthStencilState(Optional.empty()).withCull(false).build();
    private static @Nullable Frame frame;
    private static @Nullable GpuBuffer uniforms;
    private static @Nullable GpuBuffer vertices;
    private static @Nullable GpuTexture atmosphere;
    private static @Nullable GpuTextureView atmosphereView;
    private static boolean active;
    private static boolean failed;

    private OverworldSkyRenderer() {
    }

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        frame = null;
        if (AnvilCraft.CLIENT_CONFIG.overworldSkyMode != OverworldSkyMode.SPECIAL
            || !Level.OVERWORLD.equals(event.getLevel().dimension()) || event.getRenderState().customSkyboxRenderer != null) return;
        var client = Minecraft.getInstance();
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(client.isPaused());
        var sky = event.getRenderState().skyRenderState;
        var state = OverworldSkyState.at(event.getLevel().getOverworldClockTime(),
            MunClientSky.partialDayTime(event.getLevel(), partialTick), sky.sunAngle / (Math.PI * 2));
        frame = new Frame(state, new Matrix4f(event.getRenderState().cameraRenderState.projectionMatrix), sky.rainBrightness);
    }

    public static boolean isActive() {
        return active;
    }

    public static void begin() {
        active = false;
        if (frame == null || failed) return;
        try {
            var device = RenderSystem.getDevice();
            if (!device.precompilePipeline(PIPELINE).isValid()) throw new IllegalStateException("Overworld sky shader did not compile");
            var target = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride
                : Objects.requireNonNull(Minecraft.getInstance().getMainRenderTarget().getColorTextureView());
            var source = target.texture();
            int width = source.getWidth(0);
            int height = source.getHeight(0);
            if (atmosphere == null || atmosphere.getWidth(0) != width || atmosphere.getHeight(0) != height
                || atmosphere.getFormat() != source.getFormat()) {
                closeAtmosphere();
                atmosphere = device.createTexture("Overworld sky atmosphere", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    source.getFormat(), width, height, 1, 1);
                atmosphereView = device.createTextureView(atmosphere);
            }
            device.createCommandEncoder().copyTextureToTexture(source, atmosphere, 0, 0, 0, 0, 0, width, height);
            active = true;
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    public static void render() {
        if (!active || frame == null) return;
        active = false;
        try (var stack = MemoryStack.stackPush()) {
            var device = RenderSystem.getDevice();
            if (uniforms == null) {
                uniforms = device.createBuffer(() -> "Overworld sky uniforms",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, 240);
            }
            if (vertices == null) {
                vertices = device.createBuffer(() -> "Overworld sky vertices",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX, 48);
            }
            var client = Minecraft.getInstance();
            final var sun = client.getAtlasManager().get(new SpriteId(Sheets.BLOCKS_MAPPER.sheet(), SUN));
            var data = stack.malloc(240);
            new Matrix4f(frame.projection).invert().get(0, data);
            new Matrix4f(RenderSystem.getModelViewMatrix()).invert().get(64, data);
            var state = frame.state;
            Vector x = state.moonNormal(new Vector(1, 0, 0));
            Vector y = state.moonNormal(new Vector(0, 1, 0));
            Vector z = state.moonNormal(new Vector(0, 0, 1));
            new Matrix4f((float) x.x(), (float) x.y(), (float) x.z(), 0,
                (float) y.x(), (float) y.y(), (float) y.z(), 0,
                (float) z.x(), (float) z.y(), (float) z.z(), 0, 0, 0, 0, 1).get(128, data);
            data.putFloat(192, (float) state.moon().x()).putFloat(196, (float) state.moon().y())
                .putFloat(200, (float) state.moon().z()).putFloat(204, frame.visibility);
            data.putFloat(208, (float) state.sun().x()).putFloat(212, (float) state.sun().y())
                .putFloat(216, (float) state.sun().z()).putFloat(220, 0);
            data.putFloat(224, sun.getU0()).putFloat(228, sun.getV0()).putFloat(232, sun.getU1()).putFloat(236, sun.getV1());
            var encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniforms.slice(), data);
            var quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            quad.addVertex(-1, -1, 0);
            quad.addVertex(1, -1, 0);
            quad.addVertex(1, 1, 0);
            quad.addVertex(-1, 1, 0);
            try (var mesh = quad.buildOrThrow()) {
                encoder.writeToBuffer(vertices.slice(), mesh.vertexBuffer());
                var target = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride
                    : Objects.requireNonNull(client.getMainRenderTarget().getColorTextureView());
                var moon = client.getTextureManager().getTexture(MOON);
                var atlas = client.getTextureManager().getTexture(Sheets.BLOCKS_MAPPER.sheet());
                var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                var indexBuffer = indices.getBuffer(6);
                try (var pass = encoder.createRenderPass(() -> "Overworld celestial sky", target, OptionalInt.empty())) {
                    pass.setPipeline(PIPELINE);
                    pass.setUniform("OverworldSky", uniforms);
                    pass.bindTexture("Sampler0", moon.getTextureView(), moon.getSampler());
                    pass.bindTexture("Sampler1", atlas.getTextureView(), atlas.getSampler());
                    pass.bindTexture("Sampler2", Objects.requireNonNull(atmosphereView), moon.getSampler());
                    pass.setVertexBuffer(0, vertices);
                    pass.setIndexBuffer(indexBuffer, indices.type());
                    pass.drawIndexed(0, 0, 6, 1);
                }
            }
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    private static void fail(RuntimeException exception) {
        failed = true;
        active = false;
        AnvilCraft.LOGGER.warn("Special Overworld sky unavailable; using vanilla sun and moon.", exception);
    }

    private static void closeAtmosphere() {
        if (atmosphereView != null) atmosphereView.close();
        if (atmosphere != null) atmosphere.close();
        atmosphereView = null;
        atmosphere = null;
    }

    private static void close() {
        closeAtmosphere();
        if (uniforms != null) uniforms.close();
        if (vertices != null) vertices.close();
        uniforms = null;
        vertices = null;
        frame = null;
        active = false;
        failed = false;
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        Minecraft.getInstance().execute(OverworldSkyRenderer::close);
    }

    @SubscribeEvent
    public static void shutdown(net.neoforged.neoforge.event.GameShuttingDownEvent event) {
        close();
    }

    private record Frame(OverworldSkyState state, Matrix4f projection, float visibility) {
    }
}
