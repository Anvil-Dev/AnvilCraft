package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** 缓存模型网格并生成级联深度贴图，地形采样成本与遮挡模型复杂度无关。 */
public final class MunShadowMap implements AutoCloseable {
    private final Map<Long, ChunkMesh> chunks = new HashMap<>();
    private final Set<Long> invalidated = new HashSet<>();
    private final Set<Long> denied = new HashSet<>();
    private List<ChunkPos> updateOrder = List.of();
    private final List<BlockEntity> dynamicCandidates = new ArrayList<>();
    private final Cascade[] cascades = {new Cascade(), new Cascade(), new Cascade()};
    private @Nullable ClientLevel level;
    private @Nullable MunLightingProfile profile;
    private @Nullable BuildJob building;
    private @Nullable MunShadowMesh dynamic;
    private long dynamicFingerprint;
    private Vec3 dynamicOrigin = Vec3.ZERO;
    private Vec3 renderOrigin = Vec3.ZERO;
    private Vec3 anchor = Vec3.ZERO;
    private int originX = Integer.MIN_VALUE;
    private int originY;
    private int originZ = Integer.MIN_VALUE;
    private int cacheVertices;
    private int readyCascades;
    private long geometryRevision;
    private boolean meshesPending = true;
    private long lastChunkCheck = Long.MIN_VALUE;
    private long lastDynamicScan = Long.MIN_VALUE;

    void prepare(ClientLevel level, Vec3 anchor, Vec3 renderOrigin, MunSolarLighting solar,
                 float partialTick, ShaderInstance shader, MunLightingProfile profile) {
        if (this.level != level || this.profile != profile) {
            this.close();
            this.level = level;
            this.profile = profile;
            this.originY = level.getMinBuildHeight();
        }
        this.anchor = anchor;
        this.renderOrigin = renderOrigin;
        int x = (((int) Math.floor(anchor.x)) & ~15) - profile.cacheRadius();
        int z = (((int) Math.floor(anchor.z)) & ~15) - profile.cacheRadius();
        if (x != this.originX || z != this.originZ) this.moveWindow(x, z, profile);
        this.updateMeshes(level, profile);
        this.refreshDynamic(level, partialTick, profile);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(anchor.x), (int) Math.floor(anchor.z));
        Vec3 center = Math.abs(ground - anchor.y) <= 16 ? new Vec3(anchor.x, ground, anchor.z) : anchor;
        this.render(solar, center, shader, profile);
        this.readyCascades = profile.cascades();
    }

    private void moveWindow(int x, int z, MunLightingProfile profile) {
        this.meshesPending = true;
        this.lastDynamicScan = Long.MIN_VALUE;
        this.originX = x;
        this.originZ = z;
        this.chunks.values().removeIf(data -> {
            if (this.contains(data.chunk.getPos())) return false;
            this.cacheVertices -= data.vertices();
            this.geometryRevision++;
            data.close();
            return true;
        });
        if (this.building != null && !this.contains(this.building.chunk.getPos())) {
            this.building.close();
            this.building = null;
        }
        this.invalidated.removeIf(key -> !this.contains(new ChunkPos(key)));
        this.denied.clear();
        List<ChunkPos> order = new ArrayList<>();
        int side = profile.cacheRadius() / 8;
        for (int dz = 0; dz < side; dz++) {
            for (int dx = 0; dx < side; dx++) order.add(new ChunkPos((x >> 4) + dx, (z >> 4) + dz));
        }
        order.sort(Comparator.comparingDouble(this::distance));
        this.updateOrder = order;
    }

    private double distance(ChunkPos pos) {
        double x = pos.getMiddleBlockX() - this.anchor.x;
        double z = pos.getMiddleBlockZ() - this.anchor.z;
        return x * x + z * z;
    }

    private void updateMeshes(ClientLevel level, MunLightingProfile profile) {
        if (!this.meshesPending && this.building == null && this.invalidated.isEmpty()) return;
        if (this.lastChunkCheck != level.getGameTime()) this.checkLoadedChunks(level);
        if (this.building != null && this.invalidated.contains(this.building.chunk.getPos().toLong())) {
            this.building.close();
            this.building = null;
        }
        long deadline = System.nanoTime() + profile.buildBudgetNanos();
        do {
            if (this.building == null) {
                ChunkPos next = this.nextChunk(level);
                if (next == null) {
                    this.meshesPending = false;
                    this.invalidated.clear();
                    return;
                }
                LevelChunk chunk = level.getChunkSource().getChunk(next.x, next.z, ChunkStatus.FULL, false);
                if (chunk == null) return;
                this.invalidated.remove(next.toLong());
                this.building = new BuildJob(chunk, level.getHeight(), profile.chunkVertices());
            }
            if (this.building.step(level, this.originY)) {
                this.admit(this.building.finish(), profile);
                this.building.close();
                this.building = null;
            }
        } while (System.nanoTime() < deadline);
    }

    private void checkLoadedChunks(ClientLevel level) {
        this.lastChunkCheck = level.getGameTime();
        this.chunks.values().removeIf(data -> {
            if (level.getChunkSource().getChunk(data.chunk.getPos().x, data.chunk.getPos().z, ChunkStatus.FULL, false) == data.chunk) {
                return false;
            }
            this.cacheVertices -= data.vertices();
            this.geometryRevision++;
            data.close();
            this.lastDynamicScan = Long.MIN_VALUE;
            return true;
        });
        if (this.building != null && level.getChunkSource().getChunk(
            this.building.chunk.getPos().x, this.building.chunk.getPos().z, ChunkStatus.FULL, false
        ) != this.building.chunk) {
            this.building.close();
            this.building = null;
        }
    }

    @Nullable
    private ChunkPos nextChunk(ClientLevel level) {
        for (ChunkPos pos : this.updateOrder) {
            if (this.invalidated.contains(pos.toLong())
                && level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false) != null) return pos;
        }
        for (ChunkPos pos : this.updateOrder) {
            if (this.denied.contains(pos.toLong())) continue;
            if (!this.chunks.containsKey(pos.toLong()) || this.invalidated.contains(pos.toLong())) {
                if (level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false) != null) return pos;
            }
        }
        return null;
    }

    private void admit(ChunkMesh mesh, MunLightingProfile profile) {
        long key = mesh.chunk.getPos().toLong();
        ChunkMesh old = this.chunks.get(key);
        int additional = mesh.vertices() - (old == null ? 0 : old.vertices());
        if (this.cacheVertices + additional > profile.cacheVertices()) {
            List<ChunkMesh> farthest = new ArrayList<>(this.chunks.values());
            farthest.sort(Comparator.comparingDouble((ChunkMesh value) -> this.distance(value.chunk.getPos())).reversed());
            for (ChunkMesh candidate : farthest) {
                if (this.distance(candidate.chunk.getPos()) <= this.distance(mesh.chunk.getPos())) break;
                this.chunks.remove(candidate.chunk.getPos().toLong());
                this.denied.add(candidate.chunk.getPos().toLong());
                this.cacheVertices -= candidate.vertices();
                this.geometryRevision++;
                candidate.close();
                if (this.cacheVertices + additional <= profile.cacheVertices()) break;
            }
        }
        if (this.cacheVertices + additional > profile.cacheVertices()) {
            this.denied.add(key);
            mesh.close();
            return;
        }
        if (old != null) old.close();
        this.chunks.put(key, mesh);
        this.geometryRevision++;
        this.cacheVertices += additional;
        this.lastDynamicScan = Long.MIN_VALUE;
    }

    private void refreshDynamic(ClientLevel level, float partialTick, MunLightingProfile profile) {
        if (this.lastDynamicScan != level.getGameTime()) {
            this.lastDynamicScan = level.getGameTime();
            this.dynamicCandidates.clear();
            for (ChunkMesh chunk : this.chunks.values()) {
                for (BlockEntity entity : chunk.chunk.getBlockEntities().values()) {
                    if (entity.isRemoved()
                        || entity.getBlockPos().distToCenterSqr(this.anchor) > profile.dynamicDistance() * profile.dynamicDistance()) {
                        continue;
                    }
                    if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity) instanceof ModelSelectionRenderer<?>) {
                        this.dynamicCandidates.add(entity);
                    }
                }
            }
            this.dynamicCandidates.sort(Comparator.comparingDouble(entity -> entity.getBlockPos().distToCenterSqr(this.anchor)));
        }
        List<PlacedPart> parts = new ArrayList<>();
        long fingerprint = 1;
        int count = 0;
        for (BlockEntity entity : this.dynamicCandidates) {
            if (count++ >= profile.dynamicEntities()) break;
            if (entity.isRemoved()) continue;
            if (!MunShadowMesh.castsShadow(entity.getBlockState(), level, entity.getBlockPos())) continue;
            for (SelectionPart part : ModelBlockSelection.rendererParts(entity, partialTick)) {
                PoseStack pose = new PoseStack();
                part.apply(pose);
                fingerprint = 31 * fingerprint + entity.getBlockPos().asLong();
                fingerprint = 31 * fingerprint + System.identityHashCode(part.geometry());
                fingerprint = 31 * fingerprint + pose.last().pose().hashCode();
                parts.add(new PlacedPart(entity.getBlockPos(), part));
            }
        }
        if (fingerprint == this.dynamicFingerprint) return;
        this.dynamicFingerprint = fingerprint;
        if (this.dynamic != null) this.dynamic.close();
        this.dynamic = null;
        this.dynamicOrigin = this.renderOrigin;
        if (parts.isEmpty()) return;
        try (MunShadowMesh.Builder builder = new MunShadowMesh.Builder(profile.dynamicVertices())) {
            for (PlacedPart part : parts) {
                if (builder.full()) break;
                builder.part(part.part, Vec3.atLowerCornerOf(part.position).subtract(this.dynamicOrigin));
            }
            this.dynamic = builder.finish();
        }
    }

    private void render(MunSolarLighting solar, Vec3 center, ShaderInstance shader, MunLightingProfile profile) {
        Vec3 projectedCenter = solar.project(center);
        boolean dirty = false;
        for (int index = 0; index < profile.cascades(); index++) {
            Cascade cascade = this.cascades[index];
            cascade.span = profile.radius(index) * 2;
            int resolution = cascade.target == null
                ? Math.min(profile.resolution(), RenderSystem.maxSupportedTextureSize()) : cascade.target.size();
            boolean moved = cascade.projection.update(projectedCenter, this.renderOrigin, cascade.span, resolution);
            cascade.dirty = cascade.target == null || moved || cascade.geometryRevision != this.geometryRevision
                || cascade.solarRevision != solar.shadowRevision();
            cascade.dynamicDirty = this.dynamic != null && (cascade.dirty || cascade.dynamicTarget == null
                || cascade.dynamicFingerprint != this.dynamicFingerprint);
            dirty |= cascade.dirty || cascade.dynamicDirty;
        }
        if (!dirty) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            ByteBuffer colorMask = stack.malloc(4);
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask);
            int framebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
            int readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
            int depthFunction = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC);
            boolean depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
            boolean depthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
            boolean blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
            boolean cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
            boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
            int texture = RenderSystem.getShaderTexture(0);
            try {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11C.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                RenderSystem.disableCull();
                GlStateManager._disableScissorTest();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                Uniform offset = shader.getUniform("ChunkOffset");
                if (offset == null) throw new IllegalStateException("Missing lunar shadow draw uniforms");
                for (int index = 0; index < profile.cascades(); index++) {
                    Cascade cascade = this.cascades[index];
                    if (!cascade.dirty && !cascade.dynamicDirty) continue;
                    if (cascade.target == null) cascade.target = new MunShadowTarget(profile.resolution());
                    if (cascade.dynamicDirty && cascade.dynamicTarget == null) {
                        cascade.dynamicTarget = new MunShadowTarget(profile.resolution());
                    }
                    shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, cascade.projection.matrix(), new Matrix4f(),
                        Minecraft.getInstance().getWindow());
                    solar.apply(shader);
                    shader.safeGetUniform("ShadowScale").set(2 / cascade.span, -2 / MunShadowProjection.DEPTH,
                        cascade.target.size() / cascade.span);
                    shader.apply();
                    if (cascade.dirty) {
                        cascade.target.bind();
                        for (ChunkMesh chunk : this.chunks.values()) {
                            if (chunk.mesh == null) continue;
                            Vec3 origin = new Vec3(chunk.chunk.getPos().getMinBlockX(), this.originY, chunk.chunk.getPos().getMinBlockZ());
                            this.draw(chunk.mesh, origin, cascade, solar, shader, offset);
                        }
                        cascade.geometryRevision = this.geometryRevision;
                        cascade.solarRevision = solar.shadowRevision();
                    }
                    if (cascade.dynamicDirty && this.dynamic != null && cascade.dynamicTarget != null) {
                        cascade.dynamicTarget.copyDepthFrom(cascade.target);
                        this.draw(this.dynamic, this.dynamicOrigin, cascade, solar, shader, offset);
                        cascade.dynamicFingerprint = this.dynamicFingerprint;
                    }
                    shader.clear();
                }
            } finally {
                shader.clear();
                VertexBuffer.unbind();
                RenderSystem.setShaderTexture(0, texture);
                GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, framebuffer);
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
                RenderSystem.depthFunc(depthFunction);
                RenderSystem.depthMask(depthMask);
                if (depthTest) RenderSystem.enableDepthTest();
                else RenderSystem.disableDepthTest();
                if (blend) RenderSystem.enableBlend();
                else RenderSystem.disableBlend();
                if (cull) RenderSystem.enableCull();
                else RenderSystem.disableCull();
                if (scissor) GlStateManager._enableScissorTest();
            }
        }
    }

    private void draw(MunShadowMesh mesh, Vec3 origin, Cascade cascade, MunSolarLighting solar,
                      ShaderInstance shader, Uniform offset) {
        solar.origin(shader, origin);
        offset.set(cascade.projection.offset(origin));
        offset.upload();
        mesh.draw();
    }

    public void blockChanged(ClientLevel level, BlockPos pos, BlockState previous, BlockState state) {
        if (this.level != level) return;
        if (MunShadowMesh.fullCube(previous, level, pos) && MunShadowMesh.fullCube(state, level, pos)) return;
        this.chunkChanged(new ChunkPos(pos));
    }

    public void chunkChanged(ChunkPos pos) {
        if (this.level != null && this.contains(pos)) {
            this.meshesPending = true;
            this.lastChunkCheck = Long.MIN_VALUE;
            this.lastDynamicScan = Long.MIN_VALUE;
            this.invalidated.add(pos.toLong());
            this.denied.remove(pos.toLong());
        }
    }

    private boolean contains(ChunkPos pos) {
        int x = pos.getMinBlockX() - this.originX;
        int z = pos.getMinBlockZ() - this.originZ;
        return this.profile != null && x >= 0 && x < this.profile.cacheRadius() * 2 && z >= 0 && z < this.profile.cacheRadius() * 2;
    }

    public int count() {
        return this.readyCascades;
    }

    long geometryRevision() {
        return this.geometryRevision;
    }

    int staticTextureId(int index) {
        MunShadowTarget target = this.cascades[index].target;
        return index < this.readyCascades && target != null ? target.texture() : 0;
    }

    public int textureId(int index) {
        Cascade cascade = this.cascades[index];
        MunShadowTarget target = this.dynamic == null ? cascade.target : cascade.dynamicTarget;
        return index < this.readyCascades && target != null ? target.texture() : 0;
    }

    public Matrix4f matrix(int index) {
        return this.cascades[index].projection.matrix();
    }

    public Vector3f right() {
        return this.cascades[0].projection.right();
    }

    public Vector3f up() {
        return this.cascades[0].projection.up();
    }

    public float span(int index) {
        return this.cascades[index].span;
    }

    @Override
    public void close() {
        for (Cascade cascade : this.cascades) {
            if (cascade.target != null) cascade.target.close();
            if (cascade.dynamicTarget != null) cascade.dynamicTarget.close();
            cascade.target = null;
            cascade.dynamicTarget = null;
        }
        if (this.building != null) this.building.close();
        this.building = null;
        if (this.dynamic != null) this.dynamic.close();
        this.dynamic = null;
        this.dynamicFingerprint = 0;
        this.chunks.values().forEach(ChunkMesh::close);
        this.chunks.clear();
        this.invalidated.clear();
        this.denied.clear();
        this.updateOrder = List.of();
        this.level = null;
        this.profile = null;
        this.originX = Integer.MIN_VALUE;
        this.originZ = Integer.MIN_VALUE;
        this.cacheVertices = 0;
        this.readyCascades = 0;
        this.meshesPending = true;
        this.lastChunkCheck = Long.MIN_VALUE;
        this.lastDynamicScan = Long.MIN_VALUE;
        this.dynamicCandidates.clear();
        MunShadowMesh.clearCaches();
    }

    private static final class Cascade {
        private final MunShadowProjection projection = new MunShadowProjection();
        private @Nullable MunShadowTarget target;
        private @Nullable MunShadowTarget dynamicTarget;
        private float span;
        private long geometryRevision;
        private long solarRevision;
        private boolean dirty;
        private boolean dynamicDirty;
        private long dynamicFingerprint;
    }

    private record PlacedPart(BlockPos position, SelectionPart part) {
    }

    private record ChunkMesh(LevelChunk chunk, @Nullable MunShadowMesh mesh) implements AutoCloseable {
        int vertices() {
            return this.mesh == null ? 0 : this.mesh.vertexCount();
        }

        @Override
        public void close() {
            if (this.mesh != null) this.mesh.close();
        }
    }

    private static final class BuildJob implements AutoCloseable {
        private final LevelChunk chunk;
        private final int height;
        private final int limit;
        private final int[] columns;
        private final List<BlockPos> models = new ArrayList<>();
        private int section;
        private int meshSlice;
        private int model;
        private @Nullable MunShadowMesh.Builder builder;

        private BuildJob(LevelChunk chunk, int height, int limit) {
            this.chunk = chunk;
            this.height = height;
            this.limit = limit;
            this.columns = new int[((height + 31) / 32) * 256];
        }

        boolean step(ClientLevel level, int originY) {
            if (this.section < this.chunk.getSections().length) {
                this.scan(level, originY, this.section++);
                return false;
            }
            if (this.builder == null) {
                this.builder = new MunShadowMesh.Builder(this.limit);
            }
            if (!this.builder.columns(this.columns, this.height, this.meshSlice++)) {
                return false;
            }
            if (this.model >= this.models.size() || this.builder.full()) return true;
            BlockPos pos = this.models.get(this.model++);
            BlockState state = level.getBlockState(pos);
            Vec3 offset = state.getOffset(level, pos).add(pos.getX() & 15, pos.getY() - originY, pos.getZ() & 15);
            this.builder.model(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state, pos, offset,
                state.getLightBlock(level, pos) >= level.getMaxLightLevel());
            return false;
        }

        private void scan(ClientLevel level, int originY, int index) {
            LevelChunkSection section = this.chunk.getSections()[index];
            if (section.hasOnlyAir()) return;
            if (!section.getStates().maybeHas(state -> !MunShadowMesh.fullCube(state, level, BlockPos.ZERO))) {
                int mask = 0xFFFF << ((index & 1) * 16);
                for (int column = 0; column < 256; column++) this.columns[(index >> 1) * 256 + column] |= mask;
                return;
            }
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = 0; y < 16; y++) {
                int localY = index * 16 + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir()) continue;
                        pos.set(this.chunk.getPos().getMinBlockX() + x, originY + localY, this.chunk.getPos().getMinBlockZ() + z);
                        if (!MunShadowMesh.castsShadow(state, level, pos)) continue;
                        if (MunShadowMesh.fullCube(state, level, pos)) {
                            this.columns[(localY >> 5) * 256 + z * 16 + x] |= 1 << (localY & 31);
                        } else if (state.getRenderShape() == RenderShape.MODEL && this.models.size() < this.limit / 64) {
                            this.models.add(pos.immutable());
                        }
                    }
                }
            }
        }

        ChunkMesh finish() {
            return new ChunkMesh(this.chunk, this.builder == null ? null : this.builder.finish());
        }

        @Override
        public void close() {
            if (this.builder != null) this.builder.close();
        }
    }
}
