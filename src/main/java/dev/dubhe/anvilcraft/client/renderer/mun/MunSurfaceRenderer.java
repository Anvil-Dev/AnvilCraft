package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.lwjgl.system.MemoryStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSurfaceRenderer {
    private static final ContextKey<Frame> FRAME = new ContextKey<>(AnvilCraft.of("mun_surface"));
    private static final Map<RenderPipeline, RenderPipeline> PIPELINES = new IdentityHashMap<>();
    private static final Set<RenderPipeline> REPLACEMENTS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final RenderPipeline LIGHTMAP = RenderPipelines.LIGHTMAP.toBuilder()
        .withLocation(AnvilCraft.of("pipeline/mun_lightmap")).withFragmentShader(AnvilCraft.of("core/mun/mun_lightmap")).build();
    private static final MunPostProcessing POST = new MunPostProcessing();
    private static final MunSolarLighting SOLAR = new MunSolarLighting();
    private static volatile boolean requested;
    private static boolean compiled;
    private static boolean worldPass;
    private static boolean lightmapPass;
    private static @Nullable GpuBuffer uniforms;

    private MunSurfaceRenderer() {
    }

    public static boolean lightingRequested() {
        return requested;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        boolean next = client.level != null && CelestialTravelManager.MUN_LEVEL.equals(client.level.dimension())
            && AnvilCraft.CLIENT_CONFIG.munLightingQuality == MunLightingQuality.POTATO && !IrisState.isShaderEnabled();
        if (next && !compiled) {
            try {
                if (!RenderSystem.getDevice().precompilePipeline(LIGHTMAP).isValid()) {
                    throw new IllegalStateException("Mun lightmap shader did not compile");
                }
                for (var pipeline : REPLACEMENTS) {
                    if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) {
                        throw new IllegalStateException("Mun surface shader did not compile");
                    }
                }
                POST.validate();
                compiled = true;
            } catch (RuntimeException exception) {
                MunRenderPipeline.fail(exception);
                next = false;
            }
        }
        if (next != requested) {
            requested = next;
            if (!next) POST.close();
            if (client.level != null) client.levelRenderer.allChanged();
        }
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        if (!requested || !CelestialTravelManager.MUN_LEVEL.equals(event.getLevel().dimension())) return;
        var camera = event.getRenderState().cameraRenderState.pos;
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(Minecraft.getInstance().isPaused());
        event.getRenderState().setRenderData(FRAME, new Frame(event.getLevel().getOverworldClockTime(),
            MunClientSky.partialDayTime(event.getLevel(), partial), camera,
            new org.joml.Matrix4f(event.getRenderState().cameraRenderState.projectionMatrix),
            Minecraft.getInstance().options.ambientOcclusion().get()));
    }

    public static void beginWorld(LevelRenderState state) {
        Frame frame = state.getRenderData(FRAME);
        worldPass = frame != null;
        if (frame == null) return;
        if (uniforms == null) {
            uniforms = RenderSystem.getDevice().createBuffer(() -> "Mun surface uniforms",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, 384);
        }
        Vec3 origin = Vec3.atLowerCornerOf(BlockPos.containing(frame.camera));
        SOLAR.update(frame.time, frame.partialTime, origin);
        try (var stack = MemoryStack.stackPush()) {
            var bytes = stack.malloc(384);
            SOLAR.write(bytes, frame.camera, MunLightingProfile.of(MunLightingQuality.POTATO).ambientFloor());
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniforms.slice(), bytes);
        }
    }

    @SubscribeEvent
    public static void post(net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueFeatures event) {
        Frame frame = event.getLevelRenderState().getRenderData(FRAME);
        if (worldPass && frame != null) {
            try {
                POST.render(frame.projection, MunLightingProfile.of(MunLightingQuality.POTATO), frame.ambientOcclusion);
            } catch (RuntimeException exception) {
                MunRenderPipeline.fail(exception);
                worldPass = false;
            }
        }
    }

    public static void endWorld() {
        worldPass = false;
    }

    public static void lightmap(boolean active) {
        lightmapPass = active;
    }

    public static RenderPipeline replace(RenderPipeline original) {
        if (lightmapPass && original == RenderPipelines.LIGHTMAP) return LIGHTMAP;
        if (!worldPass) return original;
        return variant(original);
    }

    private static RenderPipeline variant(RenderPipeline original) {
        String shader = original.getVertexShader().getPath();
        if (!original.getVertexShader().getNamespace().equals("minecraft")
            || !original.getFragmentShader().equals(original.getVertexShader())
            || original.getShaderDefines().flags().contains("EMISSIVE")) return original;
        if (!shader.equals("core/terrain") && !shader.equals("core/block") && !shader.equals("core/entity")) return original;
        return PIPELINES.computeIfAbsent(original, key -> {
            var builder = key.toBuilder().withLocation(AnvilCraft.of("pipeline/mun_surface_" + PIPELINES.size()))
                .withVertexShader(AnvilCraft.of("core/mun/mun_surface"))
                .withFragmentShader(AnvilCraft.of("core/mun/mun_surface"))
                .withUniform("MunSurface", UniformType.UNIFORM_BUFFER);
            if (shader.equals("core/terrain")) builder.withShaderDefine("MUN_TERRAIN");
            if (shader.equals("core/entity")) builder.withShaderDefine("MUN_ENTITY");
            var result = builder.build();
            REPLACEMENTS.add(result);
            return result;
        });
    }

    public static void bind(RenderPass pass, RenderPipeline pipeline) {
        if (worldPass && uniforms != null && REPLACEMENTS.contains(pipeline)) pass.setUniform("MunSurface", uniforms);
    }

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(LIGHTMAP);
        for (var original : new RenderPipeline[]{RenderPipelines.SOLID_TERRAIN, RenderPipelines.CUTOUT_TERRAIN,
            RenderPipelines.TRANSLUCENT_TERRAIN, RenderPipelines.SOLID_BLOCK, RenderPipelines.CUTOUT_BLOCK,
            RenderPipelines.TRANSLUCENT_BLOCK, RenderPipelines.ENTITY_SOLID, RenderPipelines.ENTITY_CUTOUT,
            RenderPipelines.ENTITY_TRANSLUCENT, RenderPipelines.ARMOR_CUTOUT_NO_CULL}) {
            event.registerPipeline(variant(original));
        }
    }

    @SubscribeEvent
    public static void reload(net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted event) {
        compiled = false;
        POST.close();
    }

    @SubscribeEvent
    public static void shutdown(GameShuttingDownEvent event) {
        POST.close();
        if (uniforms != null) uniforms.close();
        uniforms = null;
        worldPass = false;
        lightmapPass = false;
    }

    private record Frame(long time, double partialTime, Vec3 camera, org.joml.Matrix4f projection, boolean ambientOcclusion) {
    }
}
