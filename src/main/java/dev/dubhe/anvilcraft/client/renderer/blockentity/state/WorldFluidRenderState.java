package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record WorldFluidRenderState(Map<ChunkSectionLayer, List<Vertex>> layers) {
    public static WorldFluidRenderState extract(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        var models = Minecraft.getInstance().getModelManager().getFluidStateModelSet();
        var renderer = new FluidRenderer(models);
        var fluid = state.getFluidState();
        Map<ChunkSectionLayer, Capture> builders = new EnumMap<>(ChunkSectionLayer.class);
        FluidRenderer.Output output = layer -> builders.computeIfAbsent(layer, ignored -> new Capture(pos));
        var custom = models.get(fluid).customRenderer();
        if (custom == null || !custom.renderFluid(renderer, fluid, level, pos, output, state)) {
            renderer.tesselate(level, pos, output, state, fluid);
        }
        Map<ChunkSectionLayer, List<Vertex>> layers = new EnumMap<>(ChunkSectionLayer.class);
        builders.forEach((layer, builder) -> {
            builder.finishVertex();
            layers.put(layer, List.copyOf(builder.vertices));
        });
        return new WorldFluidRenderState(Map.copyOf(layers));
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector) {
        this.layers.forEach((layer, vertices) -> {
            if (vertices.isEmpty()) return;
            var type = switch (layer) {
                case SOLID -> RenderTypes.solidMovingBlock();
                case CUTOUT -> RenderTypes.cutoutMovingBlock();
                case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            };
            collector.submitCustomGeometry(pose, type, (submittedPose, output) -> {
                for (Vertex vertex : vertices) {
                    output.addVertex(submittedPose, vertex.x, vertex.y, vertex.z).setColor(vertex.color)
                        .setUv(vertex.u, vertex.v).setOverlay(vertex.overlay).setLight(vertex.light)
                        .setNormal(submittedPose, vertex.nx, vertex.ny, vertex.nz);
                }
            });
        });
    }

    public record Vertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                         float nx, float ny, float nz) {
    }

    private static final class Capture implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private final BlockPos pos;
        private boolean started;
        private float posX;
        private float posY;
        private float posZ;
        private int color = -1;
        private float textureU;
        private float textureV;
        private int overlay;
        private int light;
        private float nx;
        private float ny = 1;
        private float nz;

        private Capture(BlockPos pos) {
            this.pos = pos.immutable();
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.finishVertex();
            this.started = true;
            this.posX = x - (this.pos.getX() & 15);
            this.posY = y - (this.pos.getY() & 15);
            this.posZ = z - (this.pos.getZ() & 15);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this.setColor(alpha << 24 | red << 16 | green << 8 | blue);
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.textureU = u;
            this.textureV = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.overlay = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.light = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.nx = x;
            this.ny = y;
            this.nz = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        private void finishVertex() {
            if (!this.started) return;
            this.vertices.add(new Vertex(this.posX, this.posY, this.posZ, this.color, this.textureU, this.textureV,
                this.overlay, this.light, this.nx, this.ny, this.nz));
            this.started = false;
        }
    }
}
