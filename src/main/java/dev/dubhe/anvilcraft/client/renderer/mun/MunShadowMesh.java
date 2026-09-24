package dev.dubhe.anvilcraft.client.renderer.mun;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.mixin.client.WeightedModelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/** 阴影网格只保存模型面；几何复杂度受顶点预算限制，与屏幕上的像素数无关。 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "anvilcraft", value = net.neoforged.api.distmarker.Dist.CLIENT)
final class MunShadowMesh implements AutoCloseable {
    private static final int MAX_MODEL_QUADS = 1024;
    private static final int[] TRIANGLE_INDICES = {0, 1, 2, 0, 2, 3};
    private static final float[] BOX_VERTICES = triangulate(new SelectionGeometry(List.of(
        ConvexShape.box(new AABB(0, 0, 0, 1, 1, 1))
    )));
    private static final Cache<BlockStateModel, List<BakedQuad>> MODELS = CacheBuilder.newBuilder()
        .maximumWeight(4 * 1024 * 1024)
        .weigher((BlockStateModel model, List<BakedQuad> quads) -> quads.size() * 160 + 64).build();
    private static final Cache<ConvexShape, float[]> GEOMETRIES = CacheBuilder.newBuilder().weakKeys()
        .maximumWeight(4 * 1024 * 1024)
        .weigher((ConvexShape shape, float[] vertices) -> vertices.length * Float.BYTES + 64).build();
    private static final Cache<BlockState, Boolean> FULL_CUBES = CacheBuilder.newBuilder().maximumSize(4096).build();
    private static final Cache<TextureAtlasSprite, Boolean> OPAQUE_SPRITES = CacheBuilder.newBuilder().weakKeys().maximumSize(4096).build();
    private final int buffer;
    private final int vertexArray;
    private final int vertexCount;
    private final AABB bounds;
    private boolean closed;

    private MunShadowMesh(MeshData mesh, int vertexCount, AABB bounds) {
        this.vertexCount = vertexCount;
        this.bounds = bounds;
        try (mesh; var ignored = new MunShadowGlScope()) {
            this.buffer = GlStateManager._glGenBuffers();
            this.vertexArray = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(this.vertexArray);
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.buffer);
            GlStateManager._glBufferData(GL15C.GL_ARRAY_BUFFER, mesh.vertexBuffer(), GL15C.GL_STATIC_DRAW);
            GL20C.glEnableVertexAttribArray(0);
            GL20C.glEnableVertexAttribArray(1);
            GL20C.glEnableVertexAttribArray(2);
            GL20C.glVertexAttribPointer(0, 3, GL11C.GL_FLOAT, false, 24, 0L);
            GL20C.glVertexAttribPointer(1, 2, GL11C.GL_FLOAT, false, 24, 12L);
            GL20C.glVertexAttribPointer(2, 4, GL11C.GL_UNSIGNED_BYTE, true, 24, 20L);
        }
    }

    public void draw() {
        if (this.closed) throw new IllegalStateException("Shadow mesh is closed");
        GlStateManager._glBindVertexArray(this.vertexArray);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, this.vertexCount);
    }

    public int vertexCount() {
        return this.vertexCount;
    }

    public AABB bounds() {
        return this.bounds;
    }

    public static boolean castsShadow(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE && !state.hasBlockEntity()) return false;
        // 遮光玻璃确实阻光；普通玻璃、冰等透光材质不能因使用 cutout 而变成实心投影。
        return state.getLightDampening() >= 15
            || !(state.getBlock() instanceof HalfTransparentBlock
                || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES));
    }

    public static boolean fullCube(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock().hasDynamicShape() || state.hasOffsetFunction() || !state.isSolidRender()
            || !castsShadow(state, level, pos)) return false;
        Boolean cached = FULL_CUBES.getIfPresent(state);
        if (cached != null) return cached;
        boolean full = unitCube(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state), state, 0);
        FULL_CUBES.put(state, full);
        return full;
    }

    static boolean translucent(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getRenderShape() == RenderShape.MODEL && state.getLightDampening() < 15
            && (!castsShadow(state, level, pos) || Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state)
                    .hasMaterialFlag(level instanceof BlockAndTintGetter view ? view : BlockAndTintGetter.EMPTY,
                        pos, state, BakedQuad.FLAG_TRANSLUCENT));
    }

    private static boolean unitCube(BlockStateModel model, BlockState state, int depth) {
        if (depth > 8) return false;
        if (model instanceof WeightedModelAccessor weighted) {
            if (weighted.anvillib_cube$variants().unwrap().isEmpty()) return false;
            for (var variant : weighted.anvillib_cube$variants().unwrap()) {
                if (!unitCube(variant.value(), state, depth + 1)) return false;
            }
            return true;
        }
        if (model.getClass() != SingleVariant.class) return false;
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(0), parts);
        if (parts.size() != 1 || parts.getFirst().getClass() != SimpleModelWrapper.class) return false;
        int faces = 0;
        for (int side = 0; side <= 6; side++) {
            for (BakedQuad quad : parts.getFirst().getQuads(side == 6 ? null : Direction.from3DDataValue(side))) {
                int face = 1 << quad.direction().get3DDataValue();
                if ((faces & face) != 0) return false;
                faces |= face;
                int axis = quad.direction().getAxis().ordinal();
                int plane = quad.direction().getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
                int corners = 0;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int corner = 0;
                    for (int component = 0; component < 3; component++) {
                        float value = quad.position(vertex).get(component);
                        int integer = Math.round(value);
                        if (integer < 0 || integer > 1 || Math.abs(value - integer) > 1.0E-5F) return false;
                        if (component == axis && integer != plane) return false;
                        corner |= integer << component;
                    }
                    corners |= 1 << corner;
                }
                if (Integer.bitCount(corners) != 4) return false;
            }
        }
        return faces == 63;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void reload(net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted event) {
        clearCaches();
    }

    public static void clearCaches() {
        MODELS.invalidateAll();
        FULL_CUBES.invalidateAll();
        GEOMETRIES.invalidateAll();
        OPAQUE_SPRITES.invalidateAll();
    }

    private static boolean opaque(TextureAtlasSprite sprite) {
        Boolean cached = OPAQUE_SPRITES.getIfPresent(sprite);
        if (cached != null) return cached;
        var image = sprite.contents().getOriginalImage();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getPixel(x, y) >>> 24 == 255) continue;
                OPAQUE_SPRITES.put(sprite, false);
                return false;
            }
        }
        OPAQUE_SPRITES.put(sprite, true);
        return true;
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        GL30C.glDeleteVertexArrays(this.vertexArray);
        GlStateManager._glDeleteBuffers(this.buffer);
    }

    static final class Builder implements AutoCloseable {
        private final ByteBufferBuilder storage;
        private final BufferBuilder vertices;
        private final int limit;
        private int[] columnFaces = new int[0];
        private int count;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        Builder(int limit) {
            this.limit = limit;
            this.storage = new ByteBufferBuilder(limit * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());
            this.vertices = new BufferBuilder(this.storage, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        }

        boolean full() {
            return this.count + 36 > this.limit;
        }

        int remainingVertices() {
            return this.limit - this.count;
        }

        void model(BlockStateModel model, BlockState state, BlockPos pos, Vec3 offset, boolean opaque,
                   BlockAndTintGetter level, boolean transmission) {
            List<BakedQuad> quads = model.getClass() == SingleVariant.class ? MODELS.getIfPresent(model) : null;
            if (quads == null) {
                List<BlockStateModelPart> parts = new ArrayList<>();
                model.collectParts(level, pos, state, RandomSource.create(state.getSeed(pos)), parts);
                quads = new ArrayList<>();
                for (BlockStateModelPart part : parts) {
                    for (int side = 0; side <= 6; side++) {
                        List<BakedQuad> faces = part.getQuads(side == 6 ? null : Direction.from3DDataValue(side));
                        if (quads.size() + faces.size() > MAX_MODEL_QUADS) return;
                        quads.addAll(faces);
                    }
                }
                if (model.getClass() == SingleVariant.class && parts.size() == 1
                    && parts.getFirst().getClass() == SimpleModelWrapper.class) MODELS.put(model, List.copyOf(quads));
            }
            if (quads.isEmpty() || this.count + quads.size() * 6 > this.limit) return;
            for (BakedQuad quad : quads) {
                boolean solid = opaque;
                if (!solid && opaque(quad.materialInfo().sprite())) {
                    solid = true;
                    for (int vertex = 0; vertex < 4; vertex++) solid &= ARGB.alpha(quad.bakedColors().color(vertex)) == 255;
                }
                int tint = -1;
                if (transmission && quad.materialInfo().tintIndex() != -1) {
                    var source = Minecraft.getInstance().getBlockColors().getTintSource(state, quad.materialInfo().tintIndex());
                    if (source != null) tint = source.colorInWorld(state, level, pos);
                }
                for (int vertex : TRIANGLE_INDICES) {
                    int packed = quad.bakedColors().color(vertex);
                    int color = (packed & 0xFF000000) | (solid ? 0xFF0000 : 0);
                    if (transmission) color = (packed & 0xFF000000) | (ARGB.multiply(packed, tint) & 0xFFFFFF);
                    var point = quad.position(vertex);
                    this.coloredVertex((float) offset.x + point.x(), (float) offset.y + point.y(), (float) offset.z + point.z(),
                        UVPair.unpackU(quad.packedUV(vertex)), UVPair.unpackV(quad.packedUV(vertex)), color);
                }
            }
        }

        void fluid(BlockAndTintGetter level, BlockState state, BlockPos pos, int originY) {
            FluidVertices consumer = new FluidVertices((pos.getY() & ~15) - originY);
            new FluidRenderer(Minecraft.getInstance().getModelManager().getFluidStateModelSet())
                .tesselate(level, pos, ignored -> consumer, state, state.getFluidState());
            consumer.flush();
        }

        void part(SelectionPart part, Vec3 offset) {
            List<float[]> shapes = new ArrayList<>();
            int required = 0;
            for (ConvexShape shape : part.geometry().shapes()) {
                float[] triangles = GEOMETRIES.getIfPresent(shape);
                if (triangles == null) {
                    triangles = triangulate(new SelectionGeometry(List.of(shape)));
                    GEOMETRIES.put(shape, triangles);
                }
                shapes.add(triangles);
                required += triangles.length / 3;
            }
            if (required == 0 || this.count + required > this.limit) return;
            PoseStack pose = new PoseStack();
            pose.translate(offset.x, offset.y, offset.z);
            part.apply(pose);
            Matrix4f matrix = pose.last().pose();
            Vector3f vertex = new Vector3f();
            for (float[] triangles : shapes) {
                if (triangles.length == 0) continue;
                for (int index = 0; index < triangles.length; index += 3) {
                    matrix.transformPosition(vertex.set(triangles[index], triangles[index + 1], triangles[index + 2]));
                    this.vertex(vertex.x, vertex.y, vertex.z, 0, 0, true, 255);
                }
            }
        }

        boolean columns(int[] columns, int height, int slice) {
            if (slice >= height + 35 || this.full()) return true;
            if (this.columnFaces.length != 16 * height) this.columnFaces = new int[16 * height];
            int axis = slice < 17 ? 0 : (slice < height + 18 ? 1 : 2);
            int plane = axis == 0 ? slice : (axis == 1 ? slice - 17 : slice - height - 18);
            int rows = axis == 1 ? 16 : height;
            for (int v = 0; v < rows; v++) {
                for (int u = 0; u < 16; u++) {
                    boolean before = occupied(columns, height, axis, plane - 1, u, v);
                    boolean after = occupied(columns, height, axis, plane, u, v);
                    this.columnFaces[v * 16 + u] = before == after ? 0 : (before ? 1 : -1);
                }
            }
            this.faces(this.columnFaces, axis, plane, rows);
            return false;
        }

        private static boolean occupied(int[] columns, int height, int axis, int plane, int u, int v) {
            if (plane < 0 || plane >= (axis == 1 ? height : 16)) return false;
            int x = axis == 0 ? plane : u;
            int y = axis == 1 ? plane : v;
            int z = axis == 0 ? u : (axis == 1 ? v : plane);
            return (columns[(y >> 5) * 256 + z * 16 + x] & (1 << (y & 31))) != 0;
        }

        private void faces(int[] faces, int axis, int plane, int rows) {
            for (int v = 0; v < rows && !this.full(); v++) {
                for (int u = 0; u < 16 && !this.full(); u++) {
                    int face = faces[v * 16 + u];
                    if (face == 0) continue;
                    int width = 1;
                    while (u + width < 16 && faces[v * 16 + u + width] == face) width++;
                    int height = 1;
                    boolean extend = true;
                    while (v + height < rows && extend) {
                        for (int du = 0; du < width; du++) {
                            if (faces[(v + height) * 16 + u + du] != face) extend = false;
                        }
                        if (extend) height++;
                    }
                    for (int dv = 0; dv < height; dv++) {
                        for (int du = 0; du < width; du++) faces[(v + dv) * 16 + u + du] = 0;
                    }
                    for (int index : TRIANGLE_INDICES) {
                        float a = u + (index == 1 || index == 2 ? width : 0);
                        float b = v + (index >= 2 ? height : 0);
                        this.vertex(axis == 0 ? plane : a, axis == 1 ? plane : b,
                            axis == 0 ? a : (axis == 1 ? b : plane), 0, 0, true, 255);
                    }
                }
            }
        }

        void box(AABB box) {
            if (this.count + 36 > this.limit) return;
            for (int index = 0; index < BOX_VERTICES.length; index += 3) {
                this.vertex((float) (box.minX + box.getXsize() * BOX_VERTICES[index]),
                    (float) (box.minY + box.getYsize() * BOX_VERTICES[index + 1]),
                    (float) (box.minZ + box.getZsize() * BOX_VERTICES[index + 2]), 0, 0, true, 255);
            }
        }

        private void vertex(float x, float y, float z, float u, float v, boolean opaque, int alpha) {
            this.coloredVertex(x, y, z, u, v, alpha << 24 | (opaque ? 0xFF0000 : 0));
        }

        private void coloredVertex(float x, float y, float z, float u, float v, int color) {
            this.vertices.addVertex(x, y, z).setUv(u, v).setColor(color);
            this.count++;
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }

        @Nullable
        MunShadowMesh finish() {
            if (this.count == 0) return null;
            return new MunShadowMesh(this.vertices.buildOrThrow(), this.count,
                new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ));
        }

        @Override
        public void close() {
            this.storage.close();
        }

        private final class FluidVertices implements VertexConsumer {
            private final float[] quad = new float[20];
            private final int[] colors = new int[4];
            private final int heightOffset;
            private int size;

            private FluidVertices(int heightOffset) {
                this.heightOffset = heightOffset;
            }

            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                if (this.size == 4) this.flush();
                int index = this.size++ * 5;
                this.quad[index] = x;
                this.quad[index + 1] = y + this.heightOffset;
                this.quad[index + 2] = z;
                return this;
            }

            @Override
            public VertexConsumer setColor(int color) {
                this.colors[this.size - 1] = color;
                return this;
            }

            @Override
            public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                this.colors[this.size - 1] = alpha << 24 | red << 16 | green << 8 | blue;
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                this.quad[(this.size - 1) * 5 + 3] = u;
                this.quad[(this.size - 1) * 5 + 4] = v;
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float width) {
                return this;
            }

            private void flush() {
                if (this.size == 4 && Builder.this.count + 6 <= Builder.this.limit) {
                    for (int vertex : TRIANGLE_INDICES) {
                        int index = vertex * 5;
                        Builder.this.coloredVertex(this.quad[index], this.quad[index + 1], this.quad[index + 2],
                            this.quad[index + 3], this.quad[index + 4], this.colors[vertex]);
                    }
                }
                this.size = 0;
            }
        }
    }

    static float[] triangulate(SelectionGeometry geometry) {
        List<Vec3> triangles = new ArrayList<>();
        for (ConvexShape shape : geometry.shapes()) {
            for (ConvexShape.Face face : shape.faces()) {
                List<Vec3> vertices = new ArrayList<>();
                for (Vec3 vertex : shape.vertices()) {
                    if (Math.abs(face.signedDistance(vertex)) < 1.0E-5) vertices.add(vertex);
                }
                if (vertices.size() < 3) continue;
                Vec3 center = Vec3.ZERO;
                for (Vec3 vertex : vertices) center = center.add(vertex.scale(1.0 / vertices.size()));
                Vec3 origin = center;
                Vec3 tangent = vertices.getFirst().subtract(center).normalize();
                Vec3 bitangent = face.normal().cross(tangent);
                vertices.sort(Comparator.comparingDouble(vertex -> {
                    Vec3 direction = vertex.subtract(origin);
                    return Math.atan2(direction.dot(bitangent), direction.dot(tangent));
                }));
                for (int index = 1; index + 1 < vertices.size(); index++) {
                    triangles.add(vertices.getFirst());
                    triangles.add(vertices.get(index));
                    triangles.add(vertices.get(index + 1));
                }
                if (triangles.size() > 32768) return new float[0];
            }
        }
        float[] result = new float[triangles.size() * 3];
        for (int index = 0; index < triangles.size(); index++) {
            Vec3 vertex = triangles.get(index);
            result[index * 3] = (float) vertex.x;
            result[index * 3 + 1] = (float) vertex.y;
            result[index * 3 + 2] = (float) vertex.z;
        }
        return result;
    }
}
