package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/// 天体渲染器 —— 提供行星体、大气、星晕和行星环的渲染方法。
/// 光照模型使用 Lambert / 类 BRDF 近似计算面片颜色，
/// 大气透明度由视线方向与法线点的锐角边缘增强。
@SuppressWarnings("checkstyle:MultipleVariableDeclarations")
public class CelestialBodyRenderer {

    private static final Vector3f LIGHT_DIR = new Vector3f(0.7f, 0.5f, 0.5f).normalize();

    private static int computeLambertColor(PoseStack.Pose pose, float nx, float ny, float nz, Vector3f lightDir) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float dot = normal.dot(lightDir);
        float brightness = 0.3f + 0.7f / (1.0f + (float) Math.exp(-20.0 * (dot + 0.08)));
        int c = (int) (brightness * 255);
        return (255 << 24) | (c << 16) | (c << 8) | c;
    }

    public static float computeAtmosphereAlpha(
        PoseStack.Pose pose,
        float nx,
        float ny,
        float nz,
        float baseAlpha,
        float viewX,
        float viewY,
        float viewZ
    ) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float viewDot = Math.abs(normal.x * viewX + normal.y * viewY + normal.z * viewZ);
        float rim = 1.0f - viewDot;
        return baseAlpha * (1.0f + 3.0f * rim);
    }

    /// === 公共渲染方法 ===

    /// 按模型 JSON 声明的 render_type 渲染独立天体模型。
    public static void renderCustomModel(
        PoseStack.Pose pose,
        MultiBufferSource bufferSource,
        BakedModel model,
        int packedOverlay
    ) {
        ModelData modelData = ModelData.EMPTY;
        for (RenderType renderType : model.getRenderTypes(
            Blocks.IRON_BARS.defaultBlockState(),
            RandomSource.create(42L),
            modelData
        )) {
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose,
                bufferSource.getBuffer(renderType),
                null,
                model,
                1.0f,
                1.0f,
                1.0f,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                modelData,
                renderType
            );
        }
    }

    public static void renderPlanetBody(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        renderPlanetCube(ps, vc, light, overlay, LIGHT_DIR);
    }

    /// 用末地折跃门渲染类型渲染天体主体 —— 表面是跟随玩家视角变化的虚空效果，不需要烘焙贴图。
    /// 该渲染类型只接受顶点坐标，因此不能复用带颜色和 UV 的立方体辅助方法。
    public static void renderEndGatewayBody(PoseStack ps, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.endGateway());
        PoseStack.Pose pose = ps.last();
        /// 顶点绕序取各面外法线的逆时针方向，使背面剔除下六个面都可见。
        endGatewayQuad(consumer, pose, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1); // 下
        endGatewayQuad(consumer, pose, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0); // 上
        endGatewayQuad(consumer, pose, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0); // 北
        endGatewayQuad(consumer, pose, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1); // 南
        endGatewayQuad(consumer, pose, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0); // 西
        endGatewayQuad(consumer, pose, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1); // 东
    }

    private static void endGatewayQuad(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3
    ) {
        consumer.addVertex(pose, x0, y0, z0);
        consumer.addVertex(pose, x1, y1, z1);
        consumer.addVertex(pose, x2, y2, z2);
        consumer.addVertex(pose, x3, y3, z3);
    }

    /// === 大气颜色 ===

    public static float[] getAtmosphereColor(Temperature temperature) {
        return switch (temperature) {
            case FREEZING -> new float[]{
                0.4f,
                0.6f,
                0.9f
            };
            case COLD -> new float[]{
                0.5f,
                0.7f,
                0.9f
            };
            case MILD -> new float[]{
                0.6f,
                0.8f,
                1.0f
            };
            case HOT -> new float[]{
                0.9f,
                0.5f,
                0.3f
            };
            case SCORCHED -> new float[]{
                1.0f,
                0.3f,
                0.1f
            };
        };
    }

    public static float[] getAtmosphereColor(ColorRGBA color) {
        int rgb = color.rgba();
        return new float[]{
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f
        };
    }

    /// === 大气渲染 ===

    public static void renderAtmosphere(PoseStack ps, MultiBufferSource bufferSource, Temperature temp, int light, int overlay, long seed) {
        renderAtmosphere(ps, bufferSource, getAtmosphereColor(temp), light, overlay, seed);
    }

    public static void renderAtmosphere(PoseStack ps, MultiBufferSource bufferSource, ColorRGBA color, int light, int overlay, long seed) {
        renderAtmosphere(ps, bufferSource, getAtmosphereColor(color), light, overlay, seed);
    }

    private static void renderAtmosphere(PoseStack ps, MultiBufferSource bufferSource, float[] rgb, int light, int overlay, long seed) {
        BakedModel cubeModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState());
        VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CELESTIAL_ATMOSPHERE);
        RandomSource random = RandomSource.create(seed);
        PoseStack.Pose pose = ps.last();

        /// 计算视图方向
        Vector3f bodyCenter = new Vector3f(0.5f, 0.5f, 0.5f);
        bodyCenter.mulPosition(pose.pose());
        float vx = -bodyCenter.x, vy = -bodyCenter.y, vz = -bodyCenter.z;
        float vlen = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vlen > 1e-6f) {
            vx /= vlen;
            vy /= vlen;
            vz /= vlen;
        }

        for (Direction dir : Direction.values()) {
            float alpha = computeAtmosphereAlpha(pose, dir.getStepX(), dir.getStepY(), dir.getStepZ(), 0.2f, vx, vy, vz);
            for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(pose, quad, rgb[0], rgb[1], rgb[2], alpha, light, overlay);
            }
        }
        for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(pose, quad, rgb[0], rgb[1], rgb[2], 0.2f, light, overlay);
        }
    }

    /// === 星晕渲染 ===

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public static void renderStarHalo(PoseStack ps, MultiBufferSource bufferSource, StarData star, int light, int overlay, long seed) {
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        BakedModel cubeModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState());
        RandomSource random = RandomSource.create(seed);
        int haloIterations = 10;
        for (int i = 0; i < haloIterations; i++) {
            float progress = (float) i / haloIterations;
            float haloScale = 1.0f + progress * 0.6f;
            float alpha = (1.2f - 1.125f * progress) / haloIterations;
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(haloScale, haloScale, haloScale);
            ps.translate(-0.5, -0.5, -0.5);
            VertexConsumer consumer = bufferSource.getBuffer(ModRenderTypes.CELESTIAL_ATMOSPHERE);
            for (Direction dir : Direction.values()) {
                for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                    consumer.putBulkData(ps.last(), quad, rgb[0], rgb[1], rgb[2], alpha, light, overlay);
                }
            }
            for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(ps.last(), quad, rgb[0], rgb[1], rgb[2], alpha, light, overlay);
            }
            ps.popPose();
        }
    }

    /// === 行星环渲染 ===

    public static void renderRing(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        float y = 0.5f;
        float rmin = -0.5f;
        float rmax = 1.5f;
        float eps = 0.001f;
        PoseStack.Pose pose = ps.last();

        vc.addVertex(pose, rmin, y + eps, rmax).setColor(-1).setUv(0, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmax, y + eps, rmax).setColor(-1).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmax, y + eps, rmin).setColor(-1).setUv(0.5f, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmin, y + eps, rmin).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);

        vc.addVertex(pose, rmin, y - eps, rmin).setColor(-1).setUv(0.5f, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rmax, y - eps, rmin).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rmax, y - eps, rmax).setColor(-1).setUv(1, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rmin, y - eps, rmax)
            .setColor(-1)
            .setUv(0.5f, 0.5f)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, -1, 0);
    }

    /// === 立方体几何 ===

    @SuppressWarnings("checkstyle:LocalVariableName")
    private static void renderPlanetCube(
        PoseStack ps,
        VertexConsumer vc,
        int light,
        int overlay,
        @Nullable Vector3f lightDir
    ) {
        float x1 = 0, x2 = 1, y1 = 0, y2 = 1, z1 = 0, z2 = 1;
        PoseStack.Pose pose = ps.last();
        boolean lit = lightDir != null;

        int upColor = lit ? computeLambertColor(pose, 0, 1, 0, lightDir) : -1;
        faceUp(ps, vc, x1, x2, z1, z2, y2, 16f / 64, 0, 32f / 64, 16f / 64, light, overlay, upColor);

        int downColor = lit ? computeLambertColor(pose, 0, -1, 0, lightDir) : -1;
        vc.addVertex(pose, x1, y1, z1)
            .setColor(downColor)
            .setUv(16f / 64, 48f / 64)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y1, z1)
            .setColor(downColor)
            .setUv(32f / 64, 48f / 64)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y1, z2)
            .setColor(downColor)
            .setUv(32f / 64, 32f / 64)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y1, z2)
            .setColor(downColor)
            .setUv(16f / 64, 32f / 64)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, -1, 0);

        int nColor = lit ? computeLambertColor(pose, 0, 0, -1, lightDir) : -1;
        faceNorth(ps, vc, x1, x2, y1, y2, z1, 48f / 64, 16f / 64, 64f / 64, 32f / 64, light, overlay, nColor);
        int eColor = lit ? computeLambertColor(pose, 1, 0, 0, lightDir) : -1;
        faceEast(ps, vc, x2, y1, y2, z1, z2, 32f / 64, 16f / 64, 48f / 64, 32f / 64, light, overlay, eColor);
        int wColor = lit ? computeLambertColor(pose, -1, 0, 0, lightDir) : -1;
        faceWest(ps, vc, x1, y1, y2, z1, z2, 0, 16f / 64, 16f / 64, 32f / 64, light, overlay, wColor);
        int sColor = lit ? computeLambertColor(pose, 0, 0, 1, lightDir) : -1;
        faceSouth(ps, vc, x1, x2, y1, y2, z2, 16f / 64, 16f / 64, 32f / 64, 32f / 64, light, overlay, sColor);
    }

    /// === 面辅助方法 ===

    private static void faceUp(
        PoseStack ps,
        VertexConsumer vc,
        float x1,
        float x2,
        float z1,
        float z2,
        float y,
        float u1,
        float v1,
        float u2,
        float v2,
        int light,
        int overlay,
        int color
    ) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y, z2).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y, z2).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y, z1).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x1, y, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static void faceDown(
        PoseStack ps,
        VertexConsumer vc,
        float x1,
        float x2,
        float z1,
        float z2,
        float y,
        float u1,
        float v1,
        float u2,
        float v2,
        int light,
        int overlay,
        int color
    ) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y, z1).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y, z1).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y, z2).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y, z2).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }

    private static void faceSouth(
        PoseStack ps,
        VertexConsumer vc,
        float x1,
        float x2,
        float y1,
        float y2,
        float z,
        float u1,
        float v1,
        float u2,
        float v2,
        int light,
        int overlay,
        int color
    ) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x2, y1, z).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x2, y2, z).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x1, y2, z).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private static void faceNorth(
        PoseStack ps,
        VertexConsumer vc,
        float x1,
        float x2,
        float y1,
        float y2,
        float z,
        float u1,
        float v1,
        float u2,
        float v2,
        int light,
        int overlay,
        int color
    ) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x2, y1, z).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y1, z).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y2, z).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x2, y2, z).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
    }

    private static void faceEast(
        PoseStack ps,
        VertexConsumer vc,
        float x,
        float y1,
        float y2,
        float z1,
        float z2,
        float u1,
        float v1,
        float u2,
        float v2,
        int light,
        int overlay,
        int color
    ) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x, y1, z2).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y1, z1).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y2, z1).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y2, z2).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
    }

    private static void faceWest(
        PoseStack ps,
        VertexConsumer vc,
        float x,
        float y1,
        float y2,
        float z1,
        float z2,
        float u1,
        float v1,
        float u2,
        float v2,
        int light,
        int overlay,
        int color
    ) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x, y1, z1).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y1, z2).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y2, z2).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y2, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
    }
}
