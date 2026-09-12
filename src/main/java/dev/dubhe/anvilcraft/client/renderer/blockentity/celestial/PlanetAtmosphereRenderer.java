package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import javax.annotation.Nullable;

import static net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.LEQUAL_DEPTH_TEST;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import static net.minecraft.client.renderer.RenderStateShard.NO_DEPTH_TEST;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_EYES_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class PlanetAtmosphereRenderer {
    private static final CelestialRenderState STATE = new CelestialRenderState();
    private static final int[] CORNERS = {0, 1, 3, 2};
    private static final RenderType PORTABLE = RenderType.create(
        "anvilcraft:planet_atmosphere_portable", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 49152, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );
    @Nullable
    private static ShaderInstance atmosphereShader;

    private PlanetAtmosphereRenderer() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        STATE.beginReload();
        atmosphereShader = null;
        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("planet_atmosphere"),
                DefaultVertexFormat.POSITION);
            if (GlStateManager.glGetProgrami(shader.getId(), GL20C.GL_LINK_STATUS) == GL20C.GL_FALSE) {
                shader.close();
                STATE.fail();
                return;
            }
            event.registerShader(shader, loaded -> {
                atmosphereShader = loaded;
                STATE.completeReload();
            });
        } catch (IOException | RuntimeException exception) {
            STATE.fail();
            AnvilCraft.LOGGER.warn("Planet atmosphere shader unavailable; using vanilla atmosphere rendering.", exception);
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, float[] color, int overlay) {
        if (!STATE.standard(AnvilCraft.CLIENT_CONFIG.planetAtmosphereRenderingMode, IrisState.isShaderEnabled())) {
            VanillaCelestialRenderer.atmosphere(poseStack, buffers, color, overlay);
            return;
        }
        try {
            renderStandard(poseStack, buffers, color, overlay);
        } catch (RuntimeException exception) {
            if (STATE.fail()) AnvilCraft.LOGGER.warn("Planet atmosphere failed; using vanilla atmosphere rendering.", exception);
            VanillaCelestialRenderer.atmosphere(poseStack, buffers, color, overlay);
        }
    }

    private static void renderStandard(PoseStack poseStack, MultiBufferSource buffers, float[] color, int overlay) {
        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        Matrix4f inverse = new Matrix4f(pose).invert();
        Vector3f camera = inverse.transformPosition(new Vector3f()).sub(0.5f, 0.5f, 0.5f);
        Vector3f light = CelestialBodyRenderer.localLightDirection(inverse);
        if (!camera.isFinite() || !light.isFinite() || !Float.isFinite(camera.lengthSquared())) {
            throw new IllegalStateException("Invalid planet atmosphere transform");
        }
        ShaderInstance shader = atmosphereShader;
        if (shader == null || IrisState.isShaderEnabled() || !(buffers instanceof MultiBufferSource.BufferSource)) {
            renderPortable(poseStack, buffers, camera, light, color, overlay);
            return;
        }
        // Each queued draw owns its planet transform; another anvil cannot overwrite its uniforms before flushing.
        float[] tint = color.clone();
        boolean inside = Math.max(Math.abs(camera.x), Math.max(Math.abs(camera.y), Math.abs(camera.z))) < PlanetAtmosphereMath.OUTER_SIZE;
        RenderType type = RenderType.create(
            "anvilcraft:planet_atmosphere", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> {
                    shader.safeGetUniform("PlanetPose").set(pose);
                    shader.safeGetUniform("CameraLocal").set(camera);
                    shader.safeGetUniform("LightLocal").set(light);
                    shader.safeGetUniform("AtmosphereColor").set(tint[0], tint[1], tint[2]);
                    shader.safeGetUniform("Thickness").set(PlanetAtmosphereMath.THICKNESS);
                    return shader;
                }))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(inside ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .createCompositeState(false)
        );
        VertexConsumer consumer = buffers.getBuffer(type);
        Vector3f point = new Vector3f();
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int corner : CORNERS) {
                    facePoint(axis, side, (corner & 1) == 0 ? -PlanetAtmosphereMath.OUTER_SIZE
                        : PlanetAtmosphereMath.OUTER_SIZE, (corner & 2) == 0 ? -PlanetAtmosphereMath.OUTER_SIZE
                        : PlanetAtmosphereMath.OUTER_SIZE, point);
                    consumer.addVertex(point.x, point.y, point.z);
                }
            }
        }
    }

    private static void renderPortable(
        PoseStack poseStack, MultiBufferSource buffers, Vector3f camera, Vector3f light, float[] color, int overlay
    ) {
        float[] grid = PlanetAtmosphereMath.grid(camera.lengthSquared());
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
            .getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
        VertexConsumer consumer = buffers.getBuffer(PORTABLE);
        Vector3f point = new Vector3f();
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                if (camera.get(axis) * side <= PlanetAtmosphereMath.OUTER_SIZE) continue;
                PlanetAtmosphereMath.Haze[][] samples = new PlanetAtmosphereMath.Haze[grid.length][grid.length];
                for (int u = 0; u < grid.length; u++) {
                    for (int v = 0; v < grid.length; v++) {
                        Vector3f direction = facePoint(axis, side, grid[u], grid[v], point).sub(camera).normalize();
                        samples[u][v] = PlanetAtmosphereMath.sample(camera, direction, light);
                    }
                }
                for (int u = 0; u < grid.length - 1; u++) {
                    for (int v = 0; v < grid.length - 1; v++) {
                        for (int corner : CORNERS) {
                            int cu = u + (corner & 1);
                            int cv = v + ((corner >> 1) & 1);
                            facePoint(axis, side, grid[cu], grid[cv], point).add(0.5f, 0.5f, 0.5f);
                            PlanetAtmosphereMath.Haze haze = samples[cu][cv];
                            consumer.addVertex(poseStack.last(), point.x, point.y, point.z)
                                .setColor(color[0] * haze.light(), color[1] * haze.light(), color[2] * haze.light(), haze.opacity())
                                .setUv(sprite.getU(0.5f), sprite.getV(0.5f)).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT)
                                .setNormal(poseStack.last(), 0, 1, 0);
                        }
                    }
                }
            }
        }
    }

    private static Vector3f facePoint(int axis, int side, float u, float v, Vector3f point) {
        point.zero();
        point.setComponent(axis, side * PlanetAtmosphereMath.OUTER_SIZE);
        point.setComponent((axis + 1) % 3, u);
        point.setComponent((axis + 2) % 3, v * side);
        return point;
    }
}
