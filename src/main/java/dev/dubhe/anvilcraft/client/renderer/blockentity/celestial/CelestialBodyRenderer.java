package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

public class CelestialBodyRenderer {

    private static final Vector3f LIGHT_DIR = new Vector3f(0.7f, 0.5f, 0.5f).normalize();

    private static int computeLambertColor(PoseStack.Pose pose, float nx, float ny, float nz) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(pose.normal());
        normal.normalize();
        float dot = normal.dot(LIGHT_DIR);
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

    public static void renderPlanetBody(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        renderPlanetCube(ps, vc, light, overlay);
    }

    private static void renderPlanetCube(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        float x1 = 0;
        float x2 = 1;
        float y1 = 0;
        float y2 = 1;
        float z1 = 0;
        float z2 = 1;
        PoseStack.Pose pose = ps.last();

        faceUp(ps, vc, x1, x2, z1, z2, y2, 16f / 64, 0, 32f / 64, 16f / 64, light, overlay, computeLambertColor(pose, 0, 1, 0));

        int downColor = computeLambertColor(pose, 0, -1, 0);
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

        faceNorth(ps, vc, x1, x2, y1, y2, z1, 48f / 64, 16f / 64, 64f / 64, 32f / 64, light, overlay, computeLambertColor(pose, 0, 0, -1));
        faceEast(ps, vc, x2, y1, y2, z1, z2, 32f / 64, 16f / 64, 48f / 64, 32f / 64, light, overlay, computeLambertColor(pose, 1, 0, 0));
        faceWest(ps, vc, x1, y1, y2, z1, z2, 0, 16f / 64, 16f / 64, 32f / 64, light, overlay, computeLambertColor(pose, -1, 0, 0));
        faceSouth(ps, vc, x1, x2, y1, y2, z2, 16f / 64, 16f / 64, 32f / 64, 32f / 64, light, overlay, computeLambertColor(pose, 0, 0, 1));
    }

    public static void renderStarBody(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        // Same 1×1 cube as planets, but with full-texture UVs (star uses DynamicTexture)
        renderFullCube(ps, vc, 0, 0, 0, 1, 1, 1, light, overlay);
    }

    private static void renderFullCube(
        PoseStack ps,
        VertexConsumer vc,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        int light,
        int overlay
    ) {
        faceNorth(ps, vc, x1, x2, y1, y2, z1, 0, 0, 1, 1, light, overlay, -1);
        faceSouth(ps, vc, x1, x2, y1, y2, z2, 0, 0, 1, 1, light, overlay, -1);
        faceEast(ps, vc, x2, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, -1);
        faceWest(ps, vc, x1, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay, -1);
        faceUp(ps, vc, x1, x2, z1, z2, y2, 0, 0, 1, 1, light, overlay, -1);
        faceDown(ps, vc, x1, x2, z1, z2, y1, 0, 0, 1, 1, light, overlay, -1);
    }

    /**
     * Renders the celestial ring as a standalone flat disc model.
     * The ring is positioned at y=0.5 (equator) and extends from -0.5 to 1.5 in x/z,
     * making it a 2x2 disc centered on the body.
     */
    public static void renderRing(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        float y = 0.5f;
        float rmin = -0.5f;
        float rmax = 1.5f;
        float eps = 0.001f;
        PoseStack.Pose pose = ps.last();

        // Top face (upward normal) — top-left quadrant of texture
        vc.addVertex(pose, rmin, y + eps, rmax).setColor(-1).setUv(0, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmax, y + eps, rmax).setColor(-1).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmax, y + eps, rmin).setColor(-1).setUv(0.5f, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rmin, y + eps, rmin).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);

        // Bottom face (downward normal) — top-right quadrant of texture
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
