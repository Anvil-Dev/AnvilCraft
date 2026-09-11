package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Vector;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 用原版顶点颜色与贴图 shader 绘制月球天空，不分配专用 framebuffer 或 shader。 */
final class MunVanillaSkyRenderer {
    private static final ResourceLocation EARTH = ResourceLocation.fromNamespaceAndPath(
        AnvilCraft.MOD_ID, "textures/block/celestial_body/planet_overworld.png"
    );
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final int[][] FACES = {{1, 3, 7, 5}, {0, 4, 6, 2}, {2, 6, 7, 3}, {0, 1, 5, 4}, {4, 5, 7, 6}, {0, 2, 3, 1}};
    private static final Vector[] NORMALS = {
        new Vector(1, 0, 0), new Vector(-1, 0, 0), new Vector(0, 1, 0),
        new Vector(0, -1, 0), new Vector(0, 0, 1), new Vector(0, 0, -1)
    };
    private static final int[][] TILES = {{0, 1}, {2, 1}, {1, 0}, {1, 2}, {3, 1}, {1, 1}};
    private static final List<List<SkyVertex>> STARS = createStars();

    private MunVanillaSkyRenderer() {
    }

    static void render(double x, double z, long time, double partialTick, float daylight, Matrix4f view, Matrix4f projection) {
        ShaderInstance previousShader = RenderSystem.getShader();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        float[] color = RenderSystem.getShaderColor().clone();
        int texture = RenderSystem.getShaderTexture(0);
        boolean blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        boolean cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        boolean depth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        boolean depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        int depthFunction = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC);
        int sourceRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
        double[] depthRange = new double[2];
        GL11C.glGetDoublev(GL11C.GL_DEPTH_RANGE, depthRange);
        RenderSystem.getModelViewStack().pushMatrix();
        try {
            RenderSystem.getModelViewStack().identity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11C.GL_LEQUAL);
            GL11C.glDepthRange(1, 1);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            drawBackground();
            RenderSystem.getModelViewStack().set(view);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            MunSkyMath.Rotation rotation = MunSkyMath.skyRotation(x, z, time, partialTick);
            drawStars(rotation, time, partialTick, daylight);
            drawSun(rotation, time, partialTick);
            drawEarth(rotation, time, partialTick);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunction);
            GL11C.glDepthRange(depthRange[0], depthRange[1]);
        }
    }

    private static void drawBackground() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(-1, -1, 0).setColor(0, 0, 0, 255);
        buffer.addVertex(1, -1, 0).setColor(0, 0, 0, 255);
        buffer.addVertex(1, 1, 0).setColor(0, 0, 0, 255);
        buffer.addVertex(-1, 1, 0).setColor(0, 0, 0, 255);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawStars(MunSkyMath.Rotation rotation, long time, double partialTick, float daylight) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        float brightness = 0.85F - 0.55F * daylight;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (List<SkyVertex> star : STARS) {
            List<SkyVertex> vertices = new ArrayList<>(4);
            for (SkyVertex vertex : star) {
                vertices.add(new SkyVertex(rotation.apply(solarDirection(vertex.position(), time, partialTick)), 0, 0));
            }
            emit(buffer, clipHorizon(vertices), brightness, false);
        }
        draw(buffer);
    }

    static Vector solarDirection(Vector vector, long time, double partialTick) {
        Vector sun = MunSkyMath.referenceSun(time, partialTick);
        return new Vector(sun.y() * vector.x() + sun.x() * vector.y(),
            -sun.x() * vector.x() + sun.y() * vector.y(), vector.z());
    }

    private static void drawSun(MunSkyMath.Rotation rotation, long time, double partialTick) {
        double size = MunSkyMath.SUN_HALF_SIZE;
        List<SkyVertex> vertices = new ArrayList<>(4);
        for (int corner = 0; corner < 4; corner++) {
            float u = corner == 0 || corner == 3 ? 0 : 1;
            float v = corner < 2 ? 0 : 1;
            Vector point = new Vector((u * 2 - 1) * size, 1, (v * 2 - 1) * size);
            vertices.add(new SkyVertex(rotation.apply(solarDirection(point, time, partialTick)), u, v));
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SUN);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        emit(buffer, clipHorizon(vertices), 1, true);
        draw(buffer);
        RenderSystem.disableBlend();
    }

    private static void drawEarth(MunSkyMath.Rotation rotation, long time, double partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, EARTH);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector sun = MunSkyMath.referenceSun(time, partialTick);
        for (int face = 0; face < FACES.length; face++) {
            Vector normal = MunSkyMath.EARTH_ROTATION.apply(MunSkyMath.earthSpin(time, partialTick).apply(NORMALS[face]));
            if (normal.dot(MunSkyMath.UP) + MunSkyMath.EARTH_HALF_SIZE >= 0) continue;
            float light = (float) (0.36 + 0.64 * Math.sqrt(Math.max(0, normal.dot(sun))));
            emit(buffer, earthFace(face, rotation, time, partialTick), light, true);
        }
        draw(buffer);
    }

    static List<SkyVertex> earthFace(int face, MunSkyMath.Rotation rotation, long time, double partialTick) {
        List<SkyVertex> vertices = new ArrayList<>(4);
        for (int corner : FACES[face]) {
            float x = corner & 1;
            float y = (corner >> 1) & 1;
            float z = (corner >> 2) & 1;
            float u = switch (face) {
                case 0 -> 1 - z;
                case 1 -> z;
                case 5 -> 1 - x;
                default -> x;
            };
            float v = face == 2 ? z : face == 3 ? 1 - z : 1 - y;
            vertices.add(new SkyVertex(rotation.apply(MunSkyMath.earthCorner(corner, time, partialTick)),
                (TILES[face][0] + 0.001F + u * 0.998F) / 4, (TILES[face][1] + 0.001F + v * 0.998F) / 4));
        }
        return clipHorizon(vertices);
    }

    static List<SkyVertex> clipHorizon(List<SkyVertex> vertices) {
        List<SkyVertex> clipped = new ArrayList<>(5);
        if (vertices.isEmpty()) return clipped;
        SkyVertex previous = vertices.getLast();
        for (SkyVertex current : vertices) {
            double first = previous.position().y();
            double second = current.position().y();
            if ((first >= 0) != (second >= 0)) {
                double fraction = first / (first - second);
                Vector point = previous.position().scale(1 - fraction).add(current.position().scale(fraction));
                clipped.add(new SkyVertex(new Vector(point.x(), 0, point.z()),
                    (float) (previous.u() + (current.u() - previous.u()) * fraction),
                    (float) (previous.v() + (current.v() - previous.v()) * fraction)));
            }
            if (second >= 0) clipped.add(current);
            previous = current;
        }
        return clipped;
    }

    private static void emit(BufferBuilder buffer, List<SkyVertex> vertices, float light, boolean textured) {
        for (int index = 1; index < vertices.size() - 1; index++) {
            for (SkyVertex vertex : List.of(vertices.getFirst(), vertices.get(index), vertices.get(index + 1))) {
                Vector point = vertex.position();
                buffer.addVertex((float) (point.x() * 100), (float) (point.y() * 100), (float) (point.z() * 100));
                if (textured) buffer.setUv(vertex.u(), vertex.v());
                buffer.setColor(light, light, light, 1);
            }
        }
    }

    private static void draw(BufferBuilder buffer) {
        var mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private static List<List<SkyVertex>> createStars() {
        Random random = new Random(10842);
        List<List<SkyVertex>> stars = new ArrayList<>();
        for (int index = 0; index < 1500; index++) {
            Vector point = new Vector(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
            double length = point.dot(point);
            if (length < 0.01 || length > 1) continue;
            Vector center = point.scale(1 / Math.sqrt(length));
            Vector tangent = center.cross(Math.abs(center.y()) > 0.9 ? new Vector(1, 0, 0) : MunSkyMath.UP);
            tangent = tangent.scale(1 / Math.sqrt(tangent.dot(tangent)));
            Vector vertical = center.cross(tangent);
            double size = 0.001 + random.nextDouble() * 0.001;
            stars.add(List.of(
                new SkyVertex(center.add(tangent.scale(-size)).add(vertical.scale(-size)), 0, 0),
                new SkyVertex(center.add(tangent.scale(size)).add(vertical.scale(-size)), 0, 0),
                new SkyVertex(center.add(tangent.scale(size)).add(vertical.scale(size)), 0, 0),
                new SkyVertex(center.add(tangent.scale(-size)).add(vertical.scale(size)), 0, 0)
            ));
        }
        return List.copyOf(stars);
    }

    record SkyVertex(Vector position, float u, float v) {
    }
}
