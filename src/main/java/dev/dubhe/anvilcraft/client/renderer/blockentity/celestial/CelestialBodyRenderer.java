package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@SuppressWarnings(
    {
        "checkstyle:MultipleVariableDeclarations",
        "checkstyle:WhitespaceAround",
        "checkstyle:LocalVariableName",
        "checkstyle:Indentation",
        "LineLength"
    }
)
public class CelestialBodyRenderer {

    public static void renderPlanetBody(
        PoseStack ps, VertexConsumer vc, int light, int overlay
    ) {
        renderPlanetCube(ps, vc, light, overlay);
    }

    private static void renderPlanetCube(
        PoseStack ps, VertexConsumer vc, int light, int overlay
    ) {
        float x1 = 0, x2 = 1, y1 = 0, y2 = 1, z1 = 0, z2 = 1;

        faceUp(ps, vc, x1, x2, z1, z2, y2, 16f/64, 0, 32f/64, 16f/64, light, overlay);
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y1, z1).setColor(-1).setUv(48f/64, 16f/64).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y1, z1).setColor(-1).setUv(48f/64, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y1, z2).setColor(-1).setUv(32f/64, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y1, z2).setColor(-1).setUv(32f/64, 16f/64).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        faceNorth(ps, vc, x1, x2, y1, y2, z1, 48f/64, 16f/64, 64f/64, 32f/64, light, overlay);
        faceEast(ps, vc, x2, y1, y2, z1, z2, 32f/64, 16f/64, 48f/64, 32f/64, light, overlay);
        faceWest(ps, vc, x1, y1, y2, z1, z2, 0, 16f/64, 16f/64, 32f/64, light, overlay);
        faceSouth(ps, vc, x1, x2, y1, y2, z2, 16f/64, 16f/64, 32f/64, 32f/64, light, overlay);
    }

    public static void renderStarBody(
        PoseStack ps, VertexConsumer vc, int light, int overlay
    ) {
        float min = -0.5f, max = 1.5f;
        renderFullCube(ps, vc, min, min, min, max, max, max, light, overlay);
    }

    private static void renderFullCube(
        PoseStack ps, VertexConsumer vc,
        float x1, float y1, float z1, float x2, float y2, float z2,
        int light, int overlay
    ) {
        faceNorth(ps, vc, x1, x2, y1, y2, z1, 0, 0, 1, 1, light, overlay);
        faceSouth(ps, vc, x1, x2, y1, y2, z2, 0, 0, 1, 1, light, overlay);
        faceEast(ps, vc, x2, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay);
        faceWest(ps, vc, x1, y1, y2, z1, z2, 0, 0, 1, 1, light, overlay);
        faceUp(ps, vc, x1, x2, z1, z2, y2, 0, 0, 1, 1, light, overlay);
        faceDown(ps, vc, x1, x2, z1, z2, y1, 0, 0, 1, 1, light, overlay);
    }

    /**
     * Renders the celestial ring as a standalone flat disc model.
     * The ring is positioned at y=0.5 (equator) and extends from -0.5 to 1.5 in x/z,
     * making it a 2x2 disc centered on the body.
     */
    public static void renderRing(
        PoseStack ps, VertexConsumer vc, int light, int overlay
    ) {
        float y = 0.5f, rMin = -0.5f, rMax = 1.5f, eps = 0.001f;
        PoseStack.Pose pose = ps.last();

        // Top face (upward normal) — top-left quadrant of texture
        vc.addVertex(pose, rMin, y + eps, rMax).setColor(-1).setUv(0, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rMax, y + eps, rMax).setColor(-1).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rMax, y + eps, rMin).setColor(-1).setUv(0.5f, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, rMin, y + eps, rMin).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);

        // Bottom face (downward normal) — top-right quadrant of texture
        vc.addVertex(pose, rMin, y - eps, rMin).setColor(-1).setUv(0.5f, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rMax, y - eps, rMin).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rMax, y - eps, rMax).setColor(-1).setUv(1, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, rMin, y - eps, rMax).setColor(-1).setUv(0.5f, 0.5f).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }

    private static void faceUp(PoseStack ps, VertexConsumer vc,
                                float x1, float x2, float z1, float z2, float y,
                                float u1, float v1, float u2, float v2,
                                int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y, z2).setColor(-1).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y, z2).setColor(-1).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x2, y, z1).setColor(-1).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x1, y, z1).setColor(-1).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static void faceDown(PoseStack ps, VertexConsumer vc,
                                  float x1, float x2, float z1, float z2, float y,
                                  float u1, float v1, float u2, float v2,
                                  int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y, z1).setColor(-1).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y, z1).setColor(-1).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x2, y, z2).setColor(-1).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y, z2).setColor(-1).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }

    private static void faceSouth(PoseStack ps, VertexConsumer vc,
                                   float x1, float x2, float y1, float y2, float z,
                                   float u1, float v1, float u2, float v2,
                                   int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y1, z).setColor(-1).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x2, y1, z).setColor(-1).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x2, y2, z).setColor(-1).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x1, y2, z).setColor(-1).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private static void faceNorth(PoseStack ps, VertexConsumer vc,
                                   float x1, float x2, float y1, float y2, float z,
                                   float u1, float v1, float u2, float v2,
                                   int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x2, y1, z).setColor(-1).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y1, z).setColor(-1).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y2, z).setColor(-1).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x2, y2, z).setColor(-1).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
    }

    private static void faceEast(PoseStack ps, VertexConsumer vc,
                                  float x, float y1, float y2, float z1, float z2,
                                  float u1, float v1, float u2, float v2,
                                  int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x, y1, z2).setColor(-1).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y1, z1).setColor(-1).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y2, z1).setColor(-1).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x, y2, z2).setColor(-1).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
    }

    private static void faceWest(PoseStack ps, VertexConsumer vc,
                                  float x, float y1, float y2, float z1, float z2,
                                  float u1, float v1, float u2, float v2,
                                  int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x, y1, z1).setColor(-1).setUv(u1, v2).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y1, z2).setColor(-1).setUv(u2, v2).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y2, z2).setColor(-1).setUv(u2, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x, y2, z1).setColor(-1).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
    }
}
