package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** 采集实体和方块实体的实际渲染顶点，每帧只构建一次，供所有阴影级联复用。 */
final class MunEntityShadows implements MultiBufferSource, AutoCloseable {
    private static final int MAX_BUFFERS = 64;
    private static final int BUFFER_RETENTION_FRAMES = 60;
    private static final int[] QUAD_INDICES = {0, 1, 2, 0, 2, 3};
    private static final int[] TRIANGLE_INDICES = {0, 1, 2};
    private final Map<RenderType, ShadowBuffer> buffers = new LinkedHashMap<>();
    private final List<TexturedMesh> meshes = new ArrayList<>();
    private final List<Caster> candidates = new ArrayList<>();
    private final VertexConsumer discard = new ShadowBuffer(null, 0);
    private int remainingVertices;

    void refresh(ClientLevel level, Vec3 anchor, Vec3 origin, float partialTick, MunLightingProfile profile,
                 List<BlockEntity> blockEntities) {
        this.meshes.clear();
        if (!profile.entityShadows()) {
            this.close();
            return;
        }
        ProfilerFiller profiler = level.getProfiler();
        profiler.push("munEntityShadows");
        try {
            profiler.push("collect");
            try {
                this.buffers.values().forEach(ShadowBuffer::reset);
                this.remainingVertices = profile.dynamicVertices();
                this.collect(level, anchor, profile, blockEntities);
            } finally {
                profiler.pop();
            }
            MunSurfaceRenderer.beginShadowCapture();
            try {
                this.captureCandidates(level, origin, partialTick, profile, profiler);
                profiler.push("upload");
                try {
                    this.buffers.values().forEach(ShadowBuffer::finish);
                    this.buffers.values().removeIf(buffer -> {
                        if (buffer.idleFrames <= BUFFER_RETENTION_FRAMES) return false;
                        buffer.close();
                        return true;
                    });
                } finally {
                    profiler.pop();
                }
            } finally {
                MunSurfaceRenderer.endShadowCapture();
                VertexBuffer.unbind();
            }
        } catch (RuntimeException | Error exception) {
            this.close();
            throw exception;
        } finally {
            this.candidates.clear();
            profiler.pop();
        }
    }

    private void collect(ClientLevel level, Vec3 anchor, MunLightingProfile profile, List<BlockEntity> blockEntities) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved() || entity.isInvisible() || entity.isSpectator()) continue;
            double distance = entity.getBoundingBox().distanceToSqr(anchor);
            if (distance > profile.dynamicDistance() * profile.dynamicDistance()) continue;
            this.candidates.add(new Caster(distance, entity, null));
        }
        for (BlockEntity entity : blockEntities) {
            if (entity.isRemoved() || !entity.getType().isValid(entity.getBlockState())) continue;
            if (!MunShadowMesh.castsShadow(entity.getBlockState(), level, entity.getBlockPos())) continue;
            double distance = distance(entity, anchor);
            if (distance <= profile.dynamicDistance() * profile.dynamicDistance()) this.candidates.add(new Caster(distance, null, entity));
        }
        this.candidates.sort(Comparator.comparingDouble(Caster::distance));
    }

    private void captureCandidates(ClientLevel level, Vec3 origin, float partialTick, MunLightingProfile profile, ProfilerFiller profiler) {
        profiler.push("renderers");
        try {
            for (int index = 0; index < Math.min(this.candidates.size(), profile.dynamicEntities()); index++) {
                if (this.remainingVertices < 6) break;
                Caster caster = this.candidates.get(index);
                if (caster.entity != null) {
                    profiler.push(() -> "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(caster.entity.getType()));
                    try {
                        float tick = level.tickRateManager().isEntityFrozen(caster.entity) ? 1 : partialTick;
                        this.capture(caster.entity, origin, tick);
                    } finally {
                        profiler.pop();
                    }
                } else if (caster.blockEntity != null) {
                    profiler.push(() -> "blockEntity:" + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(caster.blockEntity.getType()));
                    try {
                        this.capture(caster.blockEntity, origin, partialTick);
                    } finally {
                        profiler.pop();
                    }
                }
                this.buffers.values().forEach(ShadowBuffer::flush);
            }
        } finally {
            profiler.pop();
        }
    }

    static double distance(BlockEntity entity, Vec3 anchor) {
        BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        return renderer == null ? Double.POSITIVE_INFINITY : renderer.getRenderBoundingBox(entity).distanceToSqr(anchor);
    }

    private void capture(BlockEntity entity, Vec3 origin, float partialTick) {
        BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return;
        PoseStack pose = new PoseStack();
        pose.translate(entity.getBlockPos().getX() - origin.x, entity.getBlockPos().getY() - origin.y,
            entity.getBlockPos().getZ() - origin.z);
        renderer.render(entity, partialTick, pose, this, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    private void capture(Entity entity, Vec3 origin, float partialTick) {
        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        Vec3 offset = renderer.getRenderOffset(entity, partialTick);
        PoseStack pose = new PoseStack();
        pose.translate(Mth.lerp(partialTick, entity.xOld, entity.getX()) - origin.x + offset.x,
            Mth.lerp(partialTick, entity.yOld, entity.getY()) - origin.y + offset.y,
            Mth.lerp(partialTick, entity.zOld, entity.getZ()) - origin.z + offset.z);
        renderer.render(entity, Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick, pose, this, LightTexture.FULL_BRIGHT);
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (this.remainingVertices < 3 || type.isOutline()
            || !type.format().getElements().contains(VertexFormatElement.NORMAL)
            || type.mode() != VertexFormat.Mode.QUADS && type.mode() != VertexFormat.Mode.TRIANGLES
            || !(type instanceof RenderType.CompositeRenderType composite)) return this.discard;
        var state = composite.state();
        if (state.transparencyState != RenderStateShard.NO_TRANSPARENCY
            && state.transparencyState != RenderStateShard.TRANSLUCENT_TRANSPARENCY) return this.discard;
        if (!(state.textureState instanceof RenderStateShard.TextureStateShard texture) || texture.texture.isEmpty()) return this.discard;
        if (!this.buffers.containsKey(type) && this.buffers.size() >= MAX_BUFFERS && !this.evictIdleBuffer()) return this.discard;
        ShadowBuffer buffer = this.buffers.computeIfAbsent(type, key -> new ShadowBuffer(texture.texture.get(),
            type.mode() == VertexFormat.Mode.QUADS ? 4 : 3));
        buffer.idleFrames = 0;
        return buffer;
    }

    private boolean evictIdleBuffer() {
        RenderType oldest = null;
        int age = 0;
        for (var entry : this.buffers.entrySet()) {
            if (entry.getValue().idleFrames <= age) continue;
            oldest = entry.getKey();
            age = entry.getValue().idleFrames;
        }
        if (oldest == null) return false;
        ShadowBuffer buffer = this.buffers.remove(oldest);
        if (buffer != null) buffer.close();
        return true;
    }

    boolean isEmpty() {
        return this.meshes.isEmpty();
    }

    void draw(ShaderInstance shader) {
        for (TexturedMesh mesh : this.meshes) {
            shader.setSampler("Sampler0", Minecraft.getInstance().getTextureManager().getTexture(mesh.texture).getId());
            shader.apply();
            mesh.buffer.bind();
            mesh.buffer.draw();
        }
    }

    @Override
    public void close() {
        this.candidates.clear();
        this.meshes.clear();
        this.buffers.values().forEach(ShadowBuffer::close);
        this.buffers.clear();
    }

    private record Caster(double distance, @Nullable Entity entity, @Nullable BlockEntity blockEntity) {
    }

    private record TexturedMesh(ResourceLocation texture, VertexBuffer buffer) {
    }

    private final class ShadowBuffer implements VertexConsumer, AutoCloseable {
        private final @Nullable ResourceLocation texture;
        private final int primitiveSize;
        private final float[] primitive = new float[24];
        private @Nullable ByteBufferBuilder storage;
        private @Nullable BufferBuilder vertices;
        private @Nullable VertexBuffer buffer;
        private int count;
        private int idleFrames;

        private ShadowBuffer(@Nullable ResourceLocation texture, int primitiveSize) {
            this.texture = texture;
            this.primitiveSize = primitiveSize;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (this.texture == null) return this;
            if (this.count == this.primitiveSize) this.flush();
            int index = this.count++ * 6;
            this.primitive[index] = x;
            this.primitive[index + 1] = y;
            this.primitive[index + 2] = z;
            this.primitive[index + 3] = 0;
            this.primitive[index + 4] = 0;
            this.primitive[index + 5] = 255;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            if (this.count > 0) this.primitive[(this.count - 1) * 6 + 5] = alpha;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            if (this.count > 0) {
                this.primitive[(this.count - 1) * 6 + 3] = u;
                this.primitive[(this.count - 1) * 6 + 4] = v;
            }
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
            int[] indices = this.primitiveSize == 4 ? QUAD_INDICES : TRIANGLE_INDICES;
            if (this.count == this.primitiveSize && this.texture != null && remainingVertices >= indices.length) {
                if (this.vertices == null) {
                    if (this.storage == null) this.storage = new ByteBufferBuilder(4096);
                    this.vertices = new BufferBuilder(this.storage, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
                }
                for (int vertex : indices) {
                    int index = vertex * 6;
                    this.vertices.addVertex(this.primitive[index], this.primitive[index + 1], this.primitive[index + 2])
                        .setUv(this.primitive[index + 3], this.primitive[index + 4])
                        .setColor(0, 0, 0, (int) this.primitive[index + 5]);
                }
                remainingVertices -= indices.length;
            }
            this.count = 0;
        }

        private void finish() {
            this.flush();
            if (this.vertices == null || this.texture == null) return;
            if (this.buffer == null) this.buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            meshes.add(new TexturedMesh(this.texture, this.buffer));
            this.buffer.bind();
            this.buffer.upload(this.vertices.buildOrThrow());
        }

        private void reset() {
            this.idleFrames++;
            this.count = 0;
            this.vertices = null;
            if (this.storage != null) this.storage.clear();
        }

        @Override
        public void close() {
            if (this.buffer != null) this.buffer.close();
            this.buffer = null;
            if (this.storage != null) this.storage.close();
            this.storage = null;
            this.vertices = null;
        }
    }
}
