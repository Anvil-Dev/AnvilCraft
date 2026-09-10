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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** 使用实体渲染器的动画模型和贴图透明度生成投影，不受主相机视锥裁剪。 */
final class MunEntityShadows implements MultiBufferSource, AutoCloseable {
    private static final int[] QUAD_INDICES = {0, 1, 2, 0, 2, 3};
    private static final int[] TRIANGLE_INDICES = {0, 1, 2};
    private final Map<RenderType, ShadowBuffer> buffers = new LinkedHashMap<>();
    private final List<TexturedMesh> meshes = new ArrayList<>();
    private final VertexConsumer discard = new ShadowBuffer(null, 0);
    private int remainingVertices;

    void refresh(ClientLevel level, Vec3 anchor, Vec3 origin, float partialTick, MunLightingProfile profile) {
        this.close();
        if (!profile.entityShadows()) return;
        this.remainingVertices = profile.dynamicVertices();
        List<Entity> candidates = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved() || entity.isInvisible() || entity.isSpectator()) continue;
            if (entity.distanceToSqr(anchor) > profile.dynamicDistance() * profile.dynamicDistance()) continue;
            candidates.add(entity);
        }
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(anchor)));
        try {
            for (int index = 0; index < Math.min(candidates.size(), profile.dynamicEntities()); index++) {
                if (this.remainingVertices < 3) break;
                Entity entity = candidates.get(index);
                float tick = level.tickRateManager().isEntityFrozen(entity) ? 1 : partialTick;
                this.capture(entity, origin, tick);
                this.buffers.values().forEach(ShadowBuffer::flush);
            }
            this.buffers.values().forEach(ShadowBuffer::finish);
        } catch (RuntimeException | Error exception) {
            this.close();
            throw exception;
        } finally {
            this.buffers.values().forEach(ShadowBuffer::close);
            this.buffers.clear();
            VertexBuffer.unbind();
        }
    }

    private void capture(Entity entity, Vec3 origin, float partialTick) {
        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        Vec3 offset = renderer.getRenderOffset(entity, partialTick);
        PoseStack pose = new PoseStack();
        pose.translate(Mth.lerp(partialTick, entity.xOld, entity.getX()) - origin.x + offset.x,
            Mth.lerp(partialTick, entity.yOld, entity.getY()) - origin.y + offset.y,
            Mth.lerp(partialTick, entity.zOld, entity.getZ()) - origin.z + offset.z);
        // 直接调用模型渲染器，避免把原版圆形阴影、着火特效和调试框再次投影。
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
        return this.buffers.computeIfAbsent(type, key -> new ShadowBuffer(texture.texture.get(),
            type.mode() == VertexFormat.Mode.QUADS ? 4 : 3));
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
        this.meshes.forEach(mesh -> mesh.buffer.close());
        this.meshes.clear();
        this.buffers.values().forEach(ShadowBuffer::close);
        this.buffers.clear();
    }

    private record TexturedMesh(ResourceLocation texture, VertexBuffer buffer) {
    }

    private final class ShadowBuffer implements VertexConsumer, AutoCloseable {
        private final @Nullable ResourceLocation texture;
        private final int primitiveSize;
        private final float[] primitive = new float[24];
        private @Nullable ByteBufferBuilder storage;
        private @Nullable BufferBuilder vertices;
        private int count;

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
                    this.storage = new ByteBufferBuilder(4096);
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
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            meshes.add(new TexturedMesh(this.texture, buffer));
            buffer.bind();
            buffer.upload(this.vertices.buildOrThrow());
        }

        @Override
        public void close() {
            if (this.storage != null) this.storage.close();
            this.storage = null;
            this.vertices = null;
        }
    }
}
