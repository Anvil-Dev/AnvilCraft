package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** 每批共享紧凑的单元局部顶点，重放顶点时才选择当前帧的输出格式。 */
@NullMarked
final class PlacementVertices implements VertexConsumer {
    private static final int STRIDE = 8;
    private static final int BOTTOM_FACE = 1 << 2;
    private final IntArrayList data = new IntArrayList();
    private int vertex;
    private float dx;
    private float dy;
    private float dz;
    private boolean covered;

    int end() {
        return this.data.size();
    }

    int bake(BlueprintRenderView view, ModelBlockRenderer models, FluidRenderer fluids,
              BlockPos pos, BlockState state, Vec3 pivot, PlacementVertices extra) {
        float x = (float) (pos.getX() - pivot.x);
        float y = (float) (pos.getY() - pivot.y);
        float z = (float) (pos.getZ() - pivot.z);
        var manager = Minecraft.getInstance().getModelManager();
        view.selectUnit(pos);
        if (state.getRenderShape() == RenderShape.MODEL) {
            var pose = new PoseStack().last();
            var model = manager.getBlockStateModelSet().get(state);
            int coveredFaces = view.coveredFaces(pos, state);
            this.covered = false;
            models.tesselateBlock((ox, oy, oz, quad, instance) -> {
                var output = this.covered ? extra : this;
                output.dx = ox;
                output.dy = oy;
                output.dz = oz;
                output.putBakedQuad(pose, quad, instance);
            }, x, y, z, view, pos, state, coveredFaces == 0 ? model : this.trackFaces(model, coveredFaces), state.getSeed(pos));
        }
        int terrainEnd = this.bakeFluid(view, fluids, pos, state, x, y, z);
        this.data.addAll(extra.data);
        extra.data.clear();
        return terrainEnd;
    }

    private int bakeFluid(BlueprintRenderView view, FluidRenderer fluids, BlockPos pos, BlockState state, float x, float y, float z) {
        var fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            this.dx = x - SectionPos.sectionRelative(pos.getX());
            this.dy = y - SectionPos.sectionRelative(pos.getY());
            this.dz = z - SectionPos.sectionRelative(pos.getZ());
            FluidRenderer.Output output = ignored -> this;
            var custom = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid).customRenderer();
            if (custom != null && custom.renderFluid(fluids, fluid, view, pos, output, state)) {
                return this.end();
            }
            int first = this.end();
            fluids.tesselate(view, pos, output, state, fluid);
            int terrainEnd = this.end();
            // 保留真实世界的水面高度与流向，只为缩小后露出的方向补面。
            float[] heights = new float[4];
            Arrays.fill(heights, fluid.getType().isSame(view.getFluidState(pos.above()).getType()) ? y + 1f : Float.NaN);
            int faces = 0;
            for (int index = first; index < terrainEnd; index += 4 * STRIDE) {
                int face = this.fluidFace(index, x, y, z);
                faces |= face;
                if (face != BOTTOM_FACE) {
                    this.alignFluidHeights(index, x, y, z, heights, true);
                }
            }
            view.separateFluids(true);
            try {
                fluids.tesselate(view, pos, output, state, fluid);
            } finally {
                view.separateFluids(false);
            }
            int write = terrainEnd;
            int[] vertices = this.data.elements();
            for (int read = terrainEnd; read < this.end(); read += 4 * STRIDE) {
                int face = this.fluidFace(read, x, y, z);
                if ((faces & face) == 0) {
                    if (face != BOTTOM_FACE) {
                        this.alignFluidHeights(read, x, y, z, heights, false);
                    }
                    System.arraycopy(vertices, read, vertices, write, 4 * STRIDE);
                    write += 4 * STRIDE;
                }
            }
            this.data.size(write);
            return terrainEnd;
        }
        return this.end();
    }

    private BlockStateModel trackFaces(BlockStateModel model, int mask) {
        return new DelegateBlockStateModel(model) {
            @Override
            public void collectParts(
                BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts
            ) {
                int start = parts.size();
                super.collectParts(view, pos, state, random, parts);
                for (int index = start; index < parts.size(); index++) {
                    parts.set(index, new PlacementPart(parts.get(index), mask, PlacementVertices.this));
                }
            }
        };
    }

    // 按模型声明的剔除方向标记补面，斜面和无剔除方向的四边形都能保留原模型的行为。
    private record PlacementPart(BlockStateModelPart part, int mask, PlacementVertices vertices) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction face) {
            this.vertices.covered = face != null && (this.mask & (1 << face.ordinal())) != 0;
            return this.part.getQuads(face);
        }

        @Override
        public TriState ambientOcclusion() {
            return this.part.ambientOcclusion();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.part.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.part.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return this.part.materialFlags();
        }
    }

    // 补面的上沿与真实水面共用四角高度，避免相邻水源被分开缩放时出现缝隙。
    private void alignFluidHeights(int first, float x, float y, float z, float[] heights, boolean sample) {
        int[] values = this.data.elements();
        for (int i = first; i < first + 4 * STRIDE; i += STRIDE) {
            float height = Float.intBitsToFloat(values[i + 1]);
            if (height - y <= 0.01f) {
                continue;
            }
            int corner = (Float.intBitsToFloat(values[i]) - x > 0.5f ? 1 : 0)
                | (Float.intBitsToFloat(values[i + 2]) - z > 0.5f ? 2 : 0);
            if (sample) {
                heights[corner] = height;
            } else if (!Float.isNaN(heights[corner])) {
                values[i + 1] = Float.floatToRawIntBits(heights[corner]);
            }
        }
    }

    private int fluidFace(int first, float x, float y, float z) {
        int[] values = this.data.elements();
        for (int axis = 0; axis < 3; axis++) {
            int index = first + axis;
            int coordinate = values[index];
            if (coordinate == values[index + STRIDE] && coordinate == values[index + 2 * STRIDE]
                && coordinate == values[index + 3 * STRIDE]) {
                float origin = axis == 0 ? x : axis == 1 ? y : z;
                boolean positive = Float.intBitsToFloat(coordinate) - origin > (axis == 1 ? 0.01f : 0.5f);
                return 1 << (axis * 2 + (positive ? 1 : 0));
            }
        }
        return 1 << 3;
    }

    void draw(VertexConsumer out, int first, int end, float x, float y, float z, float scale, int alpha) {
        int[] vertices = this.data.elements();
        for (int i = first; i < end; i += STRIDE) {
            int color = vertices[i + 3];
            int normal = vertices[i + 7];
            out.addVertex(x + Float.intBitsToFloat(vertices[i]) * scale, y + Float.intBitsToFloat(vertices[i + 1]) * scale,
                z + Float.intBitsToFloat(vertices[i + 2]) * scale, ARGB.color(ARGB.alpha(color) * alpha / 255, color),
                Float.intBitsToFloat(vertices[i + 4]), Float.intBitsToFloat(vertices[i + 5]), 0, vertices[i + 6],
                (byte) normal / 127f, (byte) (normal >>> 8) / 127f, (byte) (normal >>> 16) / 127f);
        }
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.vertex = this.data.size();
        this.data.add(Float.floatToRawIntBits(x + this.dx));
        this.data.add(Float.floatToRawIntBits(y + this.dy));
        this.data.add(Float.floatToRawIntBits(z + this.dz));
        for (int i = 3; i < STRIDE; i++) {
            this.data.add(0);
        }
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        this.data.set(this.vertex + 3, color);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this.setColor(ARGB.color(alpha, red, green, blue));
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.data.set(this.vertex + 4, Float.floatToRawIntBits(u));
        this.data.set(this.vertex + 5, Float.floatToRawIntBits(v));
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.data.set(this.vertex + 6, u | (v << 16));
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.data.set(this.vertex + 7, ((byte) (x * 127) & 255) | (((byte) (y * 127) & 255) << 8) | ((byte) (z * 127) << 16));
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return this;
    }
}
