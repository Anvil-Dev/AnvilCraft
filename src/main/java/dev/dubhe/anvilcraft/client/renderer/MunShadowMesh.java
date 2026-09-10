package dev.dubhe.anvilcraft.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.mixin.client.WeightedModelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.Tags;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/** 阴影网格只保存模型面；几何复杂度受顶点预算限制，与屏幕上的像素数无关。 */
final class MunShadowMesh implements AutoCloseable {
    private static final int MAX_MODEL_QUADS = 1024;
    private static final int[] TRIANGLE_INDICES = {0, 1, 2, 0, 2, 3};
    private static final float[] BOX_VERTICES = triangulate(new SelectionGeometry(List.of(
        ConvexShape.box(new AABB(0, 0, 0, 1, 1, 1))
    )));
    private static final Cache<BlockState, List<BakedQuad>> MODELS = CacheBuilder.newBuilder()
        .maximumWeight(4 * 1024 * 1024)
        .weigher((BlockState state, List<BakedQuad> quads) -> quads.size() * 160 + 64).build();
    private static final Cache<ConvexShape, float[]> GEOMETRIES = CacheBuilder.newBuilder().weakKeys()
        .maximumWeight(4 * 1024 * 1024)
        .weigher((ConvexShape shape, float[] vertices) -> vertices.length * Float.BYTES + 64).build();
    private static final Cache<BlockState, Boolean> FULL_CUBES = CacheBuilder.newBuilder().maximumSize(4096).build();
    private static final Cache<TextureAtlasSprite, Boolean> OPAQUE_SPRITES = CacheBuilder.newBuilder().weakKeys().maximumSize(4096).build();
    private final VertexBuffer buffer;
    private final int vertexCount;
    private final AABB bounds;

    private MunShadowMesh(VertexBuffer buffer, int vertexCount, AABB bounds) {
        this.buffer = buffer;
        this.vertexCount = vertexCount;
        this.bounds = bounds;
    }

    public void draw(Matrix4f view, Matrix4f projection, ShaderInstance shader) {
        this.buffer.bind();
        this.buffer.drawWithShader(view, projection, shader);
    }

    public void draw() {
        this.buffer.bind();
        this.buffer.draw();
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
        return state.getLightBlock(level, pos) >= level.getMaxLightLevel()
            || !(state.getBlock() instanceof HalfTransparentBlock
                || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES));
    }

    public static boolean fullCube(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock().hasDynamicShape() || state.hasOffsetFunction() || !state.isSolidRender(level, pos)
            || !castsShadow(state, level, pos)) return false;
        Boolean cached = FULL_CUBES.getIfPresent(state);
        if (cached != null) return cached;
        boolean full = unitCube(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state, 0);
        FULL_CUBES.put(state, full);
        return full;
    }

    static boolean translucent(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getRenderShape() == RenderShape.MODEL && state.getLightBlock(level, pos) < level.getMaxLightLevel()
            && (!castsShadow(state, level, pos) || ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.translucent());
    }

    private static boolean unitCube(BakedModel model, BlockState state, int depth) {
        if (depth > 8) return false;
        if (model instanceof WeightedModelAccessor weighted) {
            if (weighted.anvillib_cube$variants().isEmpty()) return false;
            for (var variant : weighted.anvillib_cube$variants()) {
                if (!unitCube(variant.data(), state, depth + 1)) return false;
            }
            return true;
        }
        if (!(model instanceof SimpleBakedModel)) return false;
        int faces = 0;
        RandomSource random = RandomSource.create(0);
        for (int side = 0; side <= 6; side++) {
            List<BakedQuad> quads = model.getQuads(
                state, side == 6 ? null : Direction.from3DDataValue(side), random, ModelData.EMPTY, null
            );
            for (BakedQuad quad : quads) {
                int face = 1 << quad.getDirection().get3DDataValue();
                if ((faces & face) != 0) return false;
                faces |= face;
                int axis = quad.getDirection().getAxis().ordinal();
                int plane = quad.getDirection().getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                int corners = 0;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int corner = 0;
                    for (int component = 0; component < 3; component++) {
                        float value = Float.intBitsToFloat(vertices[vertex * stride + component]);
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
                if (image.getPixelRGBA(x, y) >>> 24 == 255) continue;
                OPAQUE_SPRITES.put(sprite, false);
                return false;
            }
        }
        OPAQUE_SPRITES.put(sprite, true);
        return true;
    }

    @Override
    public void close() {
        this.buffer.close();
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

        void model(BakedModel model, BlockState state, BlockPos pos, Vec3 offset, boolean opaque) {
            this.model(model, state, pos, offset, opaque, null);
        }

        void model(BakedModel model, BlockState state, BlockPos pos, Vec3 offset, boolean opaque, @Nullable BlockAndTintGetter level) {
            List<BakedQuad> quads = model instanceof SimpleBakedModel ? MODELS.getIfPresent(state) : null;
            if (quads == null) {
                quads = new ArrayList<>();
                RandomSource random = RandomSource.create();
                for (int side = 0; side <= 6; side++) {
                    random.setSeed(state.getSeed(pos));
                    List<BakedQuad> faces = model.getQuads(
                        state, side == 6 ? null : Direction.from3DDataValue(side), random, ModelData.EMPTY, null
                    );
                    if (quads.size() + faces.size() > MAX_MODEL_QUADS) {
                        if (model instanceof SimpleBakedModel) MODELS.put(state, List.of());
                        return;
                    }
                    quads.addAll(faces);
                }
                if (model instanceof SimpleBakedModel) MODELS.put(state, List.copyOf(quads));
            }
            if (quads.isEmpty() || this.count + quads.size() * 6 > this.limit) return;
            for (BakedQuad quad : quads) {
                int[] data = quad.getVertices();
                int stride = data.length / 4;
                boolean solid = opaque;
                if (!solid && opaque(quad.getSprite())) {
                    solid = true;
                    for (int vertex = 0; vertex < 4; vertex++) solid &= data[vertex * stride + 3] >>> 24 == 255;
                }
                for (int vertex : TRIANGLE_INDICES) {
                    int index = vertex * stride;
                    int packed = data[index + 3];
                    int color = (packed & 0xFF000000) | (solid ? 0xFF0000 : 0);
                    if (level != null) {
                        int tint = quad.isTinted()
                            ? Minecraft.getInstance().getBlockColors().getColor(state, level, pos, quad.getTintIndex()) : -1;
                        color = (packed & 0xFF000000) | ((packed & 255) * (tint >> 16 & 255) / 255) << 16
                            | ((packed >> 8 & 255) * (tint >> 8 & 255) / 255) << 8 | (packed >> 16 & 255) * (tint & 255) / 255;
                    }
                    this.coloredVertex((float) offset.x + Float.intBitsToFloat(data[index]),
                        (float) offset.y + Float.intBitsToFloat(data[index + 1]),
                        (float) offset.z + Float.intBitsToFloat(data[index + 2]),
                        Float.intBitsToFloat(data[index + 4]), Float.intBitsToFloat(data[index + 5]),
                        color);
                }
            }
        }

        void fluid(BlockAndTintGetter level, BlockState state, BlockPos pos, int originY) {
            FluidVertices consumer = new FluidVertices((pos.getY() & ~15) - originY);
            Minecraft.getInstance().getBlockRenderer().renderLiquid(pos, level, consumer, state, state.getFluidState());
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
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(this.vertices.buildOrThrow());
            VertexBuffer.unbind();
            return new MunShadowMesh(buffer, this.count, new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ));
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

            private void flush() {
                if (this.size == 4 && count + 6 <= limit) {
                    for (int vertex : TRIANGLE_INDICES) {
                        int index = vertex * 5;
                        coloredVertex(this.quad[index], this.quad[index + 1], this.quad[index + 2],
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
