package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import org.joml.Vector3f;

/**
 * Celestial body vertex rendering utilities for CFA screen preview.
 * Provides Lambert-lit cube rendering for planet bodies, atmospheres,
 * star halos, and ring planes. All methods operate on raw {@link VertexConsumer}.
 */
@SuppressWarnings("checkstyle:MultipleVariableDeclarations")
public class CelestialBodyRenderer {

    private static final Vector3f LIGHT_DIR = new Vector3f(0.7f, 0.5f, 0.5f).normalize();

    /**
     * Compute lambertian lighting color from a normal and light direction.
     */
    public static int computeLambertColor(PoseStack.Pose pose, float nx, float ny, float nz, Vector3f lightDir) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float dot = normal.dot(lightDir);
        float brightness = 0.3f + 0.7f / (1.0f + (float) Math.exp(-20.0 * (dot + 0.08)));
        int c = (int) (brightness * 255);
        return (255 << 24) | (c << 16) | (c << 8) | c;
    }

    /**
     * Compute atmosphere transparency from view angle.
     */
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

    /**
     * Get atmosphere color for a given temperature.
     */
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

    /**
     * Get RGB color for a star body.
     */
    public static float[] getStarColor(StarData star) {
        return new float[]{
            star.colorR() / 255f,
            star.colorG() / 255f,
            star.colorB() / 255f
        };
    }

    // === Public render methods ===

    /**
     * Render a textured planet cube with Lambert lighting.
     * The cube texture is a 64×48 atlas with:
     * Top face: (16,0)–(32,16)
     * Bottom:   (16,32)–(32,48)
     * North:    (48,16)–(64,32)
     * East:     (32,16)–(48,32)
     * West:     (0,16)–(16,32)
     * South:    (16,16)–(32,32)
     */
    public static void renderPlanetBody(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        renderPlanetCube(ps, vc, light, overlay, LIGHT_DIR);
    }

    /**
     * Render a translucent cube atmosphere using per-face alpha based on view angle.
     * Does NOT require a BakedModel — renders faces directly.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public static void renderAtmosphereCube(PoseStack ps, VertexConsumer vc, float[] rgb, float baseAlpha, int light, int overlay) {
        float x1 = 0, x2 = 1, y1 = 0, y2 = 1, z1 = 0, z2 = 1;
        PoseStack.Pose pose = ps.last();

        Vector3f bodyCenter = new Vector3f(0.5f, 0.5f, 0.5f);
        bodyCenter.mulPosition(pose.pose());
        float vx = -bodyCenter.x, vy = -bodyCenter.y, vz = -bodyCenter.z;
        float vlen = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vlen > 1e-6f) {
            vx /= vlen;
            vy /= vlen;
            vz /= vlen;
        }

        // Each face gets its own alpha based on view angle
        float alphaUp = computeAtmosphereAlpha(pose, 0, 1, 0, baseAlpha, vx, vy, vz);
        tintedFaceUp(ps, vc, x1, x2, z1, z2, y2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaUp);

        float alphaDown = computeAtmosphereAlpha(pose, 0, -1, 0, baseAlpha, vx, vy, vz);
        tintedFaceDown(ps, vc, x1, x2, z1, z2, y1, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaDown);

        float alphaN = computeAtmosphereAlpha(pose, 0, 0, -1, baseAlpha, vx, vy, vz);
        tintedFaceNorth(ps, vc, x1, x2, y1, y2, z1, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaN);

        float alphaS = computeAtmosphereAlpha(pose, 0, 0, 1, baseAlpha, vx, vy, vz);
        tintedFaceSouth(ps, vc, x1, x2, y1, y2, z2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaS);

        float alphaE = computeAtmosphereAlpha(pose, 1, 0, 0, baseAlpha, vx, vy, vz);
        tintedFaceEast(ps, vc, x2, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaE);

        float alphaW = computeAtmosphereAlpha(pose, -1, 0, 0, baseAlpha, vx, vy, vz);
        tintedFaceWest(ps, vc, x1, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, rgb[0], rgb[1], rgb[2], alphaW);
    }

    /**
     * Render a star halo as concentric translucent cubes.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public static void renderStarHalo(PoseStack ps, VertexConsumer vc, StarData star, int light, int overlay) {
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            float progress = (float) i / iterations;
            float scale = 1.0f + progress * 0.6f;
            float alpha = (1.2f - 1.125f * progress) / iterations;
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(scale, scale, scale);
            ps.translate(-0.5, -0.5, -0.5);
            renderAtmosphereCube(ps, vc, rgb, alpha, light, overlay);
            ps.popPose();
        }
    }

    /**
     * Render a flat ring plane at y=0.5 spanning from rmin to rmax.
     */
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

    // === Cube geometry (textured planet body) ===

    @SuppressWarnings("checkstyle:LocalVariableName")
    private static void renderPlanetCube(PoseStack ps, VertexConsumer vc, int light, int overlay, Vector3f lightDir) {
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

    // === Textured face helpers ===

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

    // === Tinted face helpers (for atmosphere/halo — constant color per face) ===

    private static void tintedFaceUp(
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
        float r,
        float g,
        float b,
        float a
    ) {
        PoseStack.Pose pose = ps.last();
        int abgr = ((int) (a * 255) << 24) | ((int) (b * 255) << 16) | ((int) (g * 255) << 8) | (int) (r * 255);
        vc.addVertex(pose, x1, y, z2).setColor(abgr).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y, z2).setColor(abgr).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y, z1).setColor(abgr).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x1, y, z1).setColor(abgr).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static void tintedFaceDown(
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
        float r,
        float g,
        float b,
        float a
    ) {
        PoseStack.Pose pose = ps.last();
        int abgr = ((int) (a * 255) << 24) | ((int) (b * 255) << 16) | ((int) (g * 255) << 8) | (int) (r * 255);
        vc.addVertex(pose, x1, y, z1).setColor(abgr).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y, z1).setColor(abgr).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y, z2).setColor(abgr).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y, z2).setColor(abgr).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }

    private static void tintedFaceNorth(
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
        float r,
        float g,
        float b,
        float a
    ) {
        PoseStack.Pose pose = ps.last();
        int abgr = ((int) (a * 255) << 24) | ((int) (b * 255) << 16) | ((int) (g * 255) << 8) | (int) (r * 255);
        vc.addVertex(pose, x2, y1, z).setColor(abgr).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y1, z).setColor(abgr).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y2, z).setColor(abgr).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x2, y2, z).setColor(abgr).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
    }

    private static void tintedFaceSouth(
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
        float r,
        float g,
        float b,
        float a
    ) {
        PoseStack.Pose pose = ps.last();
        int abgr = ((int) (a * 255) << 24) | ((int) (b * 255) << 16) | ((int) (g * 255) << 8) | (int) (r * 255);
        vc.addVertex(pose, x1, y1, z).setColor(abgr).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x2, y1, z).setColor(abgr).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x2, y2, z).setColor(abgr).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x1, y2, z).setColor(abgr).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private static void tintedFaceEast(
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
        float r,
        float g,
        float b,
        float a
    ) {
        PoseStack.Pose pose = ps.last();
        int abgr = ((int) (a * 255) << 24) | ((int) (b * 255) << 16) | ((int) (g * 255) << 8) | (int) (r * 255);
        vc.addVertex(pose, x, y1, z2).setColor(abgr).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y1, z1).setColor(abgr).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y2, z1).setColor(abgr).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y2, z2).setColor(abgr).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
    }

    private static void tintedFaceWest(
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
        float r,
        float g,
        float b,
        float a
    ) {
        PoseStack.Pose pose = ps.last();
        int abgr = ((int) (a * 255) << 24) | ((int) (b * 255) << 16) | ((int) (g * 255) << 8) | (int) (r * 255);
        vc.addVertex(pose, x, y1, z1).setColor(abgr).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y1, z2).setColor(abgr).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y2, z2).setColor(abgr).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y2, z1).setColor(abgr).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
    }
}
