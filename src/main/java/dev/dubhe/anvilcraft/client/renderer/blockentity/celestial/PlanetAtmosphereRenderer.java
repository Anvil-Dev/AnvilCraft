package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 使用原生动态变换缓冲保存每次绘制的大气局部坐标系。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class PlanetAtmosphereRenderer {
    private static final float OUTER_SIZE = 0.6875F;
    private static boolean checked;
    private static boolean failed;
    private static final int[] CORNERS = {0, 1, 3, 2};

    private PlanetAtmosphereRenderer() {
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        checked = false;
        failed = false;
    }

    public static void submit(PoseStack pose, SubmitNodeCollector collector, float[] color) {
        if (AnvilCraft.CLIENT_CONFIG.planetAtmosphereRenderingMode == CelestialRenderingMode.VANILLA || !ready()) {
            vanilla(pose, collector, color);
            return;
        }
        Matrix4f transform = new Matrix4f(pose.last().pose());
        Vector3f camera = new Matrix4f(transform).invert().transformPosition(new Vector3f()).sub(0.5F, 0.5F, 0.5F);
        if (!camera.isFinite()) {
            failed = true;
            vanilla(pose, collector, color);
            return;
        }
        boolean inside = Math.max(Math.abs(camera.x), Math.max(Math.abs(camera.y), Math.abs(camera.z))) < OUTER_SIZE;
        var pipeline = inside ? ModRenderPipelines.PLANET_ATMOSPHERE_INSIDE : ModRenderPipelines.PLANET_ATMOSPHERE;
        // 此着色器把 TextureMat 当作局部到视图的矩阵，不用于纹理采样。
        RenderType type = RenderType.create("anvilcraft_planet_atmosphere", RenderSetup.builder(pipeline)
            .setTextureTransform(new TextureTransform("anvilcraft_planet_pose", () -> transform))
            .bufferSize(512).createRenderSetup());
        final float red = color[0];
        final float green = color[1];
        final float blue = color[2];
        collector.submitCustomGeometry(pose, type, (matrix, vertices) -> {
            Vector3f point = new Vector3f();
            for (int axis = 0; axis < 3; axis++) {
                for (int side = -1; side <= 1; side += 2) {
                    for (int corner : CORNERS) {
                        point.zero();
                        point.setComponent(axis, side * OUTER_SIZE);
                        point.setComponent((axis + 1) % 3, (corner & 1) == 0 ? -OUTER_SIZE : OUTER_SIZE);
                        point.setComponent((axis + 2) % 3, ((corner & 2) == 0 ? -OUTER_SIZE : OUTER_SIZE) * side);
                        point.add(0.5F, 0.5F, 0.5F);
                        vertices.addVertex(matrix, point.x, point.y, point.z).setColor(red, green, blue, 1);
                    }
                }
            }
        });
    }

    private static boolean ready() {
        if (!checked) {
            checked = true;
            try {
                failed = !RenderSystem.getDevice().precompilePipeline(ModRenderPipelines.PLANET_ATMOSPHERE).isValid()
                    || !RenderSystem.getDevice().precompilePipeline(ModRenderPipelines.PLANET_ATMOSPHERE_INSIDE).isValid();
            } catch (RuntimeException exception) {
                failed = true;
                AnvilCraft.LOGGER.warn("Planet atmosphere pipeline unavailable; using vanilla atmosphere", exception);
            }
        }
        return !failed;
    }

    private static void vanilla(PoseStack pose, SubmitNodeCollector collector, float[] color) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(1.125F, 1.125F, 1.125F);
        pose.translate(-0.5, -0.5, -0.5);
        collector.submitCustomGeometry(pose, ModRenderTypes.CELESTIAL_ATMOSPHERE,
            (matrix, vertices) -> CelestialBodyRenderer.renderAtmosphereCube(matrix, vertices, color, 0.2F,
                LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }

}
