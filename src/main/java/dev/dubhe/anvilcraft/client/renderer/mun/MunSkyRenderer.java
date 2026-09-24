package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSkyRenderer implements CustomSkyboxRenderer {
    static final Identifier EARTH_TEXTURE = AnvilCraft.of("textures/block/celestial_body/planet_overworld.png");
    private static final Identifier SUN_TEXTURE = AnvilCraft.of("block/celestial_body/star");
    private static final MunSkyRenderer INSTANCE = new MunSkyRenderer();
    private static final ContextKey<Frame> FRAME = new ContextKey<>(AnvilCraft.of("mun_sky"));
    private static @Nullable MunSkyDraw buffers;
    private static boolean checked;
    private static boolean failed;
    private static MunLightingQuality lastQuality = MunLightingQuality.STANDARD;

    private MunSkyRenderer() {
    }

    @SubscribeEvent
    public static void register(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(AnvilCraft.of("mun"), INSTANCE);
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        Minecraft.getInstance().execute(() -> {
            closeBuffers();
            checked = false;
            failed = false;
        });
    }

    @SubscribeEvent
    public static void shutdown(net.neoforged.neoforge.event.GameShuttingDownEvent event) {
        closeBuffers();
    }

    @SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        MunClientSky.clear();
    }

    private static void closeBuffers() {
        if (buffers != null) buffers.close();
        buffers = null;
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        if (event.getRenderState().customSkyboxRenderer != INSTANCE) return;
        var camera = event.getCamera();
        if (camera.getFluidInCamera() != FogType.NONE) return;
        if (camera.entity() instanceof LivingEntity living
            && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
        var client = Minecraft.getInstance();
        MunLightingQuality quality = AnvilCraft.CLIENT_CONFIG.munLightingQuality;
        if (quality != lastQuality) {
            lastQuality = quality;
            checked = false;
            failed = false;
        }
        var position = event.getRenderState().cameraRenderState.pos;
        long time = event.getLevel().getOverworldClockTime();
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(client.isPaused());
        double partialTime = MunClientSky.partialDayTime(event.getLevel(), partialTick);
        var rotation = MunSkyMath.skyRotation(position.x, position.z, time, partialTime);
        var sunSprite = client.getAtlasManager().get(new SpriteId(Sheets.BLOCKS_MAPPER.sheet(), SUN_TEXTURE));
        event.getRenderState().setRenderData(FRAME, new Frame(position.x, position.z, time, partialTime,
            MunClientSky.sunlight(event.getLevel(), position), rotationMatrix(rotation), earthMatrix(time, partialTime),
            MunSkyMath.earthCenter(time, partialTime), MunSkyMath.referenceSun(time, partialTime),
            new Vector4f(sunSprite.getU0(), sunSprite.getV0(), sunSprite.getU1(), sunSprite.getV1()),
            AnvilCraft.CLIENT_CONFIG.munLightingQuality != MunLightingQuality.OFF));
    }

    @Override
    public boolean renderSky(LevelRenderState state, SkyRenderState sky, Matrix4fc view, Runnable setupFog) {
        Frame frame = state.getRenderData(FRAME);
        if (frame == null) return true;
        MunSkyDraw draw = buffers;
        if (draw == null) buffers = draw = new MunSkyDraw();
        var projection = state.cameraRenderState.projectionMatrix;
        if (frame.standard && !failed) {
            try {
                if (!checked) {
                    checked = true;
                    if (!RenderSystem.getDevice().precompilePipeline(MunSkyPipelines.STANDARD).isValid()) {
                        throw new IllegalStateException("Mun sky shader did not compile");
                    }
                }
                draw.upload(frame, view, projection);
                var quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                quad.addVertex(-1, -1, 0);
                quad.addVertex(1, -1, 0);
                quad.addVertex(1, 1, 0);
                quad.addVertex(-1, 1, 0);
                draw.draw(quad.buildOrThrow(), MunSkyPipelines.STANDARD, null, view, projection);
                return true;
            } catch (RuntimeException exception) {
                failed = true;
                AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.OFF;
                try {
                    saveOff();
                } catch (RuntimeException ignored) {
                    // Keep the OFF fallback active if configuration cannot be saved.
                }
                AnvilCraft.LOGGER.warn("Mun sky shader unavailable; using native sky geometry", exception);
            }
        }
        MunVanillaSkyRenderer.render(frame.x, frame.z, frame.time, frame.partialTime, frame.daylight, view, projection, draw);
        return true;
    }

    private static void saveOff() {
        for (var config : net.neoforged.fml.config.ModConfigs.getModConfigs(AnvilCraft.MOD_ID)) {
            if (config.getType() != net.neoforged.fml.config.ModConfig.Type.CLIENT
                || !(config.getSpec() instanceof net.neoforged.neoforge.common.ModConfigSpec spec)) continue;
            net.neoforged.neoforge.common.ModConfigSpec.EnumValue<MunLightingQuality> value =
                spec.getValues().get("mun_lighting_quality");
            if (value == null || config.getLoadedConfig() == null) continue;
            value.set(MunLightingQuality.OFF);
            spec.save();
        }
    }

    public static Matrix4f earthMatrix(long time, double partialTick) {
        var libration = MunSkyMath.libration(time, partialTick);
        return rotationMatrix(libration.longitude()).mul(rotationMatrix(libration.latitude()))
            .mul(rotationMatrix(MunSkyMath.EARTH_ROTATION)).mul(rotationMatrix(MunSkyMath.earthSpin(time, partialTick)));
    }

    public static Matrix4f rotationMatrix(MunSkyMath.Rotation rotation) {
        var axis = rotation.axis();
        return new Matrix4f().rotation((float) rotation.angle(), (float) axis.x(), (float) axis.y(), (float) axis.z());
    }

    public record Frame(double x, double z, long time, double partialTime, float daylight, Matrix4f skyRotation,
                        Matrix4f earthRotation, MunSkyMath.Vector earth, MunSkyMath.Vector sun, Vector4f sunUv, boolean standard) {
    }
}
