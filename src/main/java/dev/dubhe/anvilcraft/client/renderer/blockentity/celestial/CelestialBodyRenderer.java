package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

/**
 * 锻星砧天体顶点渲染工具，提供带朗伯光照的行星、大气层、恒星光晕和天体环渲染。
 * 所有方法都直接向 {@link VertexConsumer} 提交顶点。
 * 各面的顶点绕序和纹理方向必须显式保留，避免抽象后误翻转贴图或法线。
 */
@SuppressWarnings("DuplicatedCode")
public class CelestialBodyRenderer {

    private static final Vector3f LIGHT_DIR = new Vector3f(0.7f, 0.5f, 0.5f).normalize();

    /** 将浮点 RGBA 分量打包为 26.1 顶点消费者使用的 ARGB 颜色。 */
    private static int packColor(float r, float g, float b, float a) {
        return ARGB.colorFromFloat(a, r, g, b);
    }

    /** 根据法线和光照方向计算朗伯光照颜色。 */
    public static int computeLambertColor(PoseStack.Pose pose, float nx, float ny, float nz, Vector3f lightDir) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float dot = normal.dot(lightDir);
        float brightness = 0.3f + 0.7f / (1.0f + (float) Math.exp(-20.0 * (dot + 0.08)));
        int c = (int) (brightness * 255);
        return (255 << 24) | (c << 16) | (c << 8) | c;
    }

    /** 根据观察角度计算大气层透明度。 */
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

    /** 获取指定温度对应的大气层颜色。 */
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

    /** 获取恒星天体的 RGB 颜色。 */
    public static float[] getStarColor(StarData star) {
        return new float[]{
            star.colorR() / 255f,
            star.colorG() / 255f,
            star.colorB() / 255f
        };
    }

    // ==================== 公共渲染方法 ====================

    /**
     * 使用 64×48 展开贴图和朗伯光照绘制行星立方体。
     * 顶面、底面、北面、东面、西面和南面的贴图区域依次为
     * (16,0)-(32,16)、(16,32)-(32,48)、(48,16)-(64,32)、
     * (32,16)-(48,32)、(0,16)-(16,32) 和 (16,16)-(32,32)。
     */
    public static void renderPlanetBody(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
        CelestialBodyRenderer.renderPlanetCube(pose, vc, light, overlay, CelestialBodyRenderer.LIGHT_DIR);
    }

    /**
     * 根据观察角度为每个面计算透明度并直接绘制半透明立方体大气层，无需烘焙模型。
     */
    public static void renderAtmosphereCube(
        PoseStack.Pose pose,
        VertexConsumer vc,
        float[] rgb,
        float baseAlpha,
        int light,
        int overlay
    ) {
        float x1 = 0;
        float x2 = 1;
        final float y1 = 0;
        float y2 = 1;
        float z1 = 0;
        float z2 = 1;


        Vector3f bodyCenter = new Vector3f(0.5f, 0.5f, 0.5f);
        bodyCenter.mulPosition(pose.pose());
        float vx = -bodyCenter.x;
        float vy = -bodyCenter.y;
        float vz = -bodyCenter.z;
        float vlen = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vlen > 1e-6f) {
            vx /= vlen;
            vy /= vlen;
            vz /= vlen;
        }

        // 每个面根据观察角度使用独立透明度。
        float alphaUp = CelestialBodyRenderer.computeAtmosphereAlpha(pose, 0, 1, 0, baseAlpha, vx, vy, vz);
        CelestialBodyRenderer.tintedFaceUp(pose, vc, x1, x2, z1, z2, y2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaUp);

        float alphaDown = CelestialBodyRenderer.computeAtmosphereAlpha(pose, 0, -1, 0, baseAlpha, vx, vy, vz);
        CelestialBodyRenderer.tintedFaceDown(pose, vc, x1, x2, z1, z2, y1, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaDown);

        float alphaN = CelestialBodyRenderer.computeAtmosphereAlpha(pose, 0, 0, -1, baseAlpha, vx, vy, vz);
        CelestialBodyRenderer.tintedFaceNorth(pose, vc, x1, x2, y1, y2, z1, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaN);

        float alphaS = CelestialBodyRenderer.computeAtmosphereAlpha(pose, 0, 0, 1, baseAlpha, vx, vy, vz);
        CelestialBodyRenderer.tintedFaceSouth(pose, vc, x1, x2, y1, y2, z2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaS);

        float alphaE = CelestialBodyRenderer.computeAtmosphereAlpha(pose, 1, 0, 0, baseAlpha, vx, vy, vz);
        CelestialBodyRenderer.tintedFaceEast(pose, vc, x2, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaE);

        float alphaW = CelestialBodyRenderer.computeAtmosphereAlpha(pose, -1, 0, 0, baseAlpha, vx, vy, vz);
        CelestialBodyRenderer.tintedFaceWest(pose, vc, x1, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaW);
    }

    /** 使用多层同心半透明立方体绘制恒星光晕。 */
    public static void renderStarHalo(PoseStack.Pose pose, VertexConsumer vc, StarData star, int light, int overlay) {
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        int iterations = 10;
        PoseStack ps = new PoseStack();
        ps.last().set(pose);
        for (int i = 0; i < iterations; i++) {
            float progress = (float) i / iterations;
            float scale = 1.0f + progress * 0.6f;
            final float alpha = (1.2f - 1.125f * progress) / iterations;
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(scale, scale, scale);
            ps.translate(-0.5, -0.5, -0.5);
            CelestialBodyRenderer.renderAtmosphereCube(ps.last(), vc, rgb, alpha, light, overlay);
            ps.popPose();
        }
    }

    /**
     * 使用统一顶点颜色绘制六个面的不透明立方体，供恒星乘法颜色叠加层使用。
     */
    public static void renderColorCube(
        PoseStack.Pose ps,
        VertexConsumer vc,
        float r,
        float g,
        float b,
        float a,
        int light,
        int overlay
    ) {
        float x1 = 0;
        float x2 = 1;
        float y1 = 0;
        float y2 = 1;
        float z1 = 0;
        float z2 = 1;
        CelestialBodyRenderer.tintedFaceUp(ps, vc, x1, x2, z1, z2, y2, 0, 0, 1, 1, light, overlay, r, g, b, a);
        CelestialBodyRenderer.tintedFaceDown(ps, vc, x1, x2, z1, z2, y1, 0, 0, 1, 1, light, overlay, r, g, b, a);
        CelestialBodyRenderer.tintedFaceNorth(ps, vc, x1, x2, y1, y2, z1, 0, 0, 1, 1, light, overlay, r, g, b, a);
        CelestialBodyRenderer.tintedFaceSouth(ps, vc, x1, x2, y1, y2, z2, 0, 0, 1, 1, light, overlay, r, g, b, a);
        CelestialBodyRenderer.tintedFaceEast(ps, vc, x2, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, r, g, b, a);
        CelestialBodyRenderer.tintedFaceWest(ps, vc, x1, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, r, g, b, a);
    }

    /** 在 y=0.5 平面绘制从内半径延伸到外半径的扁平天体环。 */
    public static void renderRing(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
        float y = 0.5f;
        float rmin = -0.5f;
        float rmax = 1.5f;
        float eps = 0.001f;


        vc.addVertex(pose, rmin, y + eps, rmax).setColor(-1).setUv(
            0,
            0.5f
        ).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmax, y + eps, rmax).setColor(-1).setUv(
            0.5f,
            0.5f
        ).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmax, y + eps, rmin).setColor(-1).setUv(
            0.5f,
            0
        ).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmin, y + eps, rmin).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );

        vc.addVertex(pose, rmin, y - eps, rmin).setColor(-1).setUv(
            0.5f,
            0
        ).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rmax, y - eps, rmin).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            -1,
            0
        );
        vc.addVertex(pose, rmax, y - eps, rmax).setColor(-1).setUv(
            1,
            0.5f
        ).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rmin, y - eps, rmax)
            .setColor(-1)
            .setUv(0.5f, 0.5f)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, 0, -1, 0);
    }

    // ==================== 带贴图的行星立方体几何 ====================

    @SuppressWarnings("SameParameterValue")
    private static void renderPlanetCube(
        PoseStack.Pose pose,
        VertexConsumer vc,
        int light,
        int overlay,
        Vector3f lightDir
    ) {
        float x1 = 0;
        float x2 = 1;
        float y1 = 0;
        float y2 = 1;
        float z1 = 0;
        float z2 = 1;

        boolean lit = lightDir != null;

        int upColor = lit ? CelestialBodyRenderer.computeLambertColor(pose, 0, 1, 0, lightDir) : -1;
        CelestialBodyRenderer.faceUp(pose, vc, x1, x2, z1, z2, y2, 16f / 64, 0, 32f / 64, 16f / 64, light, overlay, upColor);

        int downColor = lit ? CelestialBodyRenderer.computeLambertColor(pose, 0, -1, 0, lightDir) : -1;
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

        int colorN = lit ? CelestialBodyRenderer.computeLambertColor(pose, 0, 0, -1, lightDir) : -1;
        CelestialBodyRenderer.faceNorth(pose, vc, x1, x2, y1, y2, z1, 48f / 64, 16f / 64, 64f / 64, 32f / 64, light, overlay, colorN);
        int colorE = lit ? CelestialBodyRenderer.computeLambertColor(pose, 1, 0, 0, lightDir) : -1;
        CelestialBodyRenderer.faceEast(pose, vc, x2, y1, y2, z1, z2, 32f / 64, 16f / 64, 48f / 64, 32f / 64, light, overlay, colorE);
        int colorW = lit ? CelestialBodyRenderer.computeLambertColor(pose, -1, 0, 0, lightDir) : -1;
        CelestialBodyRenderer.faceWest(pose, vc, x1, y1, y2, z1, z2, 0, 16f / 64, 16f / 64, 32f / 64, light, overlay, colorW);
        int colorS = lit ? CelestialBodyRenderer.computeLambertColor(pose, 0, 0, 1, lightDir) : -1;
        CelestialBodyRenderer.faceSouth(pose, vc, x1, x2, y1, y2, z2, 16f / 64, 16f / 64, 32f / 64, 32f / 64, light, overlay, colorS);
    }

    // ==================== 带贴图面的辅助方法 ====================

    @SuppressWarnings("SameParameterValue")
    private static void faceUp(
        PoseStack.Pose pose,
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
        vc.addVertex(pose, x1, y, z2).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
        vc.addVertex(pose, x2, y, z2).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
        vc.addVertex(pose, x2, y, z1).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
        vc.addVertex(pose, x1, y, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void faceNorth(
        PoseStack.Pose pose,
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

        vc.addVertex(pose, x2, y1, z).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
        vc.addVertex(pose, x1, y1, z).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
        vc.addVertex(pose, x1, y2, z).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
        vc.addVertex(pose, x2, y2, z).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void faceSouth(
        PoseStack.Pose pose,
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

        vc.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
        vc.addVertex(pose, x2, y1, z).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
        vc.addVertex(pose, x2, y2, z).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
        vc.addVertex(pose, x1, y2, z).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void faceEast(
        PoseStack.Pose pose,
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

        vc.addVertex(pose, x, y1, z2).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
        vc.addVertex(pose, x, y1, z1).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z1).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z2).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void faceWest(
        PoseStack.Pose pose,
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

        vc.addVertex(pose, x, y1, z1).setColor(color).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
        vc.addVertex(pose, x, y1, z2).setColor(color).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z2).setColor(color).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
    }

    // ==================== 大气层和光晕的统一着色面辅助方法 ====================

    @SuppressWarnings("SameParameterValue")
    private static void tintedFaceUp(
        PoseStack.Pose pose,
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
        float r,
        float g,
        float b,
        float a
    ) {
        int argb = CelestialBodyRenderer.packColor(r, g, b, a);
        vc.addVertex(pose, x1, y, z2).setColor(argb).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
        vc.addVertex(pose, x2, y, z2).setColor(argb).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
        vc.addVertex(pose, x2, y, z1).setColor(argb).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
        vc.addVertex(pose, x1, y, z1).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            1,
            0
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void tintedFaceDown(
        PoseStack.Pose pose,
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
        float r,
        float g,
        float b,
        float a
    ) {

        int argb = CelestialBodyRenderer.packColor(r, g, b, a);
        vc.addVertex(pose, x1, y, z1).setColor(argb).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            -1,
            0
        );
        vc.addVertex(pose, x2, y, z1).setColor(argb).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            -1,
            0
        );
        vc.addVertex(pose, x2, y, z2).setColor(argb).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            -1,
            0
        );
        vc.addVertex(pose, x1, y, z2).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            -1,
            0
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void tintedFaceNorth(
        PoseStack.Pose pose,
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
        float r,
        float g,
        float b,
        float a
    ) {
        int argb = CelestialBodyRenderer.packColor(r, g, b, a);
        vc.addVertex(pose, x2, y1, z).setColor(argb).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
        vc.addVertex(pose, x1, y1, z).setColor(argb).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
        vc.addVertex(pose, x1, y2, z).setColor(argb).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
        vc.addVertex(pose, x2, y2, z).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            -1
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void tintedFaceSouth(
        PoseStack.Pose pose,
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
        float r,
        float g,
        float b,
        float a
    ) {
        int argb = CelestialBodyRenderer.packColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z).setColor(argb).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
        vc.addVertex(pose, x2, y1, z).setColor(argb).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
        vc.addVertex(pose, x2, y2, z).setColor(argb).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
        vc.addVertex(pose, x1, y2, z).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            0,
            0,
            1
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void tintedFaceEast(
        PoseStack.Pose pose,
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
        float r,
        float g,
        float b,
        float a
    ) {
        int argb = CelestialBodyRenderer.packColor(r, g, b, a);
        vc.addVertex(pose, x, y1, z2).setColor(argb).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
        vc.addVertex(pose, x, y1, z1).setColor(argb).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z1).setColor(argb).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z2).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            1,
            0,
            0
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static void tintedFaceWest(
        PoseStack.Pose pose,
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
        float r,
        float g,
        float b,
        float a
    ) {
        int argb = CelestialBodyRenderer.packColor(r, g, b, a);
        vc.addVertex(pose, x, y1, z1).setColor(argb).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
        vc.addVertex(pose, x, y1, z2).setColor(argb).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z2).setColor(argb).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
        vc.addVertex(pose, x, y2, z1).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(
            pose,
            -1,
            0,
            0
        );
    }
}
