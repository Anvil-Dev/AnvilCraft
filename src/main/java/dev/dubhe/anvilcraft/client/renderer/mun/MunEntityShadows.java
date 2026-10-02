package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;

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
    private @Nullable MunShadowFeatures features;

    void refresh(ClientLevel level, Vec3 anchor, Vec3 origin, float partialTick, MunLightingProfile profile,
                 List<BlockEntity> blockEntities) {
        this.meshes.clear();
        if (!profile.entityShadows()) {
            this.close();
            return;
        }
        ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
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
            if (this.features == null) this.features = new MunShadowFeatures(this, this.discard);
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
                if (this.features != null) this.features.endFrame();
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
                if (this.features != null) this.features.render();
                this.buffers.values().forEach(ShadowBuffer::flush);
            }
        } finally {
            profiler.pop();
        }
    }

    static double distance(BlockEntity entity, Vec3 anchor) {
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        return renderer == null ? Double.POSITIVE_INFINITY : renderer.getRenderBoundingBox(entity).distanceToSqr(anchor);
    }

    private <E extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
        void captureBlock(E entity, float partialTick, Vec3 origin, BlockEntityRenderer<E, S> renderer) {
        if (this.features == null) return;
        var camera = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        S state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, partialTick, camera.pos, null);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        PoseStack pose = new PoseStack();
        pose.translate(entity.getBlockPos().getX() - origin.x, entity.getBlockPos().getY() - origin.y,
            entity.getBlockPos().getZ() - origin.z);
        renderer.submit(state, pose, this.features.submits, camera);
    }

    private void capture(BlockEntity entity, Vec3 origin, float partialTick) {
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer != null) this.captureBlock(entity, partialTick, origin, renderer);
    }

    private void capture(Entity entity, Vec3 origin, float partialTick) {
        if (this.features == null) return;
        var client = Minecraft.getInstance();
        var dispatcher = client.getEntityRenderDispatcher();
        var state = dispatcher.extractEntity(entity, partialTick);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        var camera = client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        dispatcher.submit(state, camera, state.x - origin.x, state.y - origin.y, state.z - origin.z,
            new PoseStack(), this.features.submits);
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (this.remainingVertices < 3 || type.isOutline()
            || type.format() != DefaultVertexFormat.BLOCK && !type.format().getElements().contains(VertexFormatElement.NORMAL)
            || type.mode() != VertexFormat.Mode.QUADS && type.mode() != VertexFormat.Mode.TRIANGLES) return this.discard;
        var blend = type.pipeline().getColorTargetState().blendFunction();
        if (blend.isPresent() && !blend.get().equals(BlendFunction.TRANSLUCENT)) return this.discard;
        var texture = type.state.textures.get("Sampler0");
        if (texture == null) return this.discard;
        if (!this.buffers.containsKey(type) && this.buffers.size() >= MAX_BUFFERS && !this.evictIdleBuffer()) return this.discard;
        ShadowBuffer buffer = this.buffers.computeIfAbsent(type, key -> new ShadowBuffer(texture.location(),
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

    void draw() {
        try (var outer = new MunShadowGlScope()) {
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            try (var ignored = new MunShadowGlScope()) {
                int sampler = GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 0);
                GL33C.glBindSampler(0, 0);
                try {
                    for (TexturedMesh mesh : this.meshes) {
                        var texture = Minecraft.getInstance().getTextureManager().getTexture(mesh.texture).getTexture();
                        if (!(texture instanceof GlTexture gl)) throw new IllegalStateException("Mun caster requires OpenGL textures");
                        GlStateManager._bindTexture(gl.glId());
                        mesh.buffer.draw();
                    }
                } finally {
                    GL33C.glBindSampler(0, sampler);
                }
            }
        }
    }

    @Override
    public void close() {
        this.candidates.clear();
        this.meshes.clear();
        if (this.features != null) this.features.close();
        this.features = null;
        this.buffers.values().forEach(ShadowBuffer::close);
        this.buffers.clear();
    }

    private record Caster(double distance, @Nullable Entity entity, @Nullable BlockEntity blockEntity) {
    }

    private record TexturedMesh(Identifier texture, MunShadowVertexBuffer buffer) {
    }

    private final class ShadowBuffer implements VertexConsumer, AutoCloseable {
        private final @Nullable Identifier texture;
        private final int primitiveSize;
        private final float[] primitive = new float[24];
        private @Nullable ByteBufferBuilder storage;
        private @Nullable BufferBuilder vertices;
        private @Nullable MunShadowVertexBuffer buffer;
        private int count;
        private int idleFrames;

        private ShadowBuffer(@Nullable Identifier texture, int primitiveSize) {
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
        public VertexConsumer setColor(int color) {
            return this.setColor(0, 0, 0, color >>> 24);
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
        public VertexConsumer setLineWidth(float width) {
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
            if (this.count == this.primitiveSize && this.texture != null && MunEntityShadows.this.remainingVertices >= indices.length) {
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
                MunEntityShadows.this.remainingVertices -= indices.length;
            }
            this.count = 0;
        }

        private void finish() {
            this.flush();
            if (this.vertices == null || this.texture == null) return;
            if (this.buffer == null) this.buffer = new MunShadowVertexBuffer(true);
            MunEntityShadows.this.meshes.add(new TexturedMesh(this.texture, this.buffer));
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
