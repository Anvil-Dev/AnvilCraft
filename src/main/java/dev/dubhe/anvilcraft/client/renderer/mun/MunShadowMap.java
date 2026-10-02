package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;

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
    private final MunEntityShadows entities = new MunEntityShadows();
    private final MunShadowProgram shader = new MunShadowProgram(false);
    private final MunShadowProgram translucentShader = new MunShadowProgram(true);
    private final Cascade[] cascades = {new Cascade(), new Cascade(), new Cascade()};
    private @Nullable ClientLevel level;
    private @Nullable MunLightingProfile profile;
    private @Nullable BuildJob building;
    private Vec3 renderOrigin = Vec3.ZERO;
    private Vec3 anchor = Vec3.ZERO;
    private int originX = Integer.MIN_VALUE;
    private int originY;
    private int originZ = Integer.MIN_VALUE;
    private int cacheVertices;
    private int cacheRadius;
    private int farRadius;
    private int readyCascades;
    private long geometryRevision;
    private boolean meshesPending = true;
    private long lastChunkCheck = Long.MIN_VALUE;
    private long lastDynamicScan = Long.MIN_VALUE;

    void prepare(ClientLevel level, Vec3 anchor, Vec3 renderOrigin, MunSolarLighting solar,
                 float partialTick, MunLightingProfile profile) {
        if (this.level != level || this.profile != profile) {
            this.close();
            this.level = level;
            this.profile = profile;
            this.originY = level.getMinY();
        }
        this.anchor = anchor;
        this.renderOrigin = renderOrigin;
        this.farRadius = Math.max(profile.distance(), Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
        int radius = this.farRadius + profile.cacheRadius() - profile.distance();
        int x = (((int) Math.floor(anchor.x)) & ~15) - radius;
        int z = (((int) Math.floor(anchor.z)) & ~15) - radius;
        if (x != this.originX || z != this.originZ || radius != this.cacheRadius) {
            this.cacheRadius = radius;
            this.moveWindow(x, z);
        }
        this.updateMeshes(level, profile);
        this.refreshDynamic(level, profile);
        this.entities.refresh(level, anchor, renderOrigin, partialTick, profile, this.dynamicCandidates);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(anchor.x), (int) Math.floor(anchor.z));
        // 高空时仍以地面为投影中心，避免太阳斜射把窗口从脚下的地形移走。
        Vec3 center = new Vec3(anchor.x, ground, anchor.z);
        this.render(solar, center, profile, level.getGameTime());
        this.readyCascades = profile.cascades();
    }

    private void moveWindow(int x, int z) {
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
        this.invalidated.removeIf(key -> !this.contains(ChunkPos.unpack(key)));
        this.denied.clear();
        List<ChunkPos> order = new ArrayList<>();
        int side = this.cacheRadius / 8;
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
        if (this.building != null && this.invalidated.contains(this.building.chunk.getPos().pack())) {
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
                LevelChunk chunk = level.getChunkSource().getChunk(next.x(), next.z(), ChunkStatus.FULL, false);
                if (chunk == null) return;
                this.invalidated.remove(next.pack());
                this.building = new BuildJob(chunk, level.getHeight(), profile.chunkVertices(), profile.translucentShadows());
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
            if (level.getChunkSource().getChunk(data.chunk.getPos().x(), data.chunk.getPos().z(), ChunkStatus.FULL, false) == data.chunk) {
                return false;
            }
            this.cacheVertices -= data.vertices();
            this.geometryRevision++;
            data.close();
            this.lastDynamicScan = Long.MIN_VALUE;
            return true;
        });
        if (this.building != null && level.getChunkSource().getChunk(
            this.building.chunk.getPos().x(), this.building.chunk.getPos().z(), ChunkStatus.FULL, false
        ) != this.building.chunk) {
            this.building.close();
            this.building = null;
        }
    }

    @Nullable
    private ChunkPos nextChunk(ClientLevel level) {
        for (ChunkPos pos : this.updateOrder) {
            if (this.invalidated.contains(pos.pack())
                && level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false) != null) return pos;
        }
        for (ChunkPos pos : this.updateOrder) {
            if (this.denied.contains(pos.pack())) continue;
            if (!this.chunks.containsKey(pos.pack()) || this.invalidated.contains(pos.pack())) {
                if (level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false) != null) return pos;
            }
        }
        return null;
    }

    private void admit(ChunkMesh mesh, MunLightingProfile profile) {
        long key = mesh.chunk.getPos().pack();
        ChunkMesh old = this.chunks.get(key);
        int additional = mesh.vertices() - (old == null ? 0 : old.vertices());
        if (this.cacheVertices + additional > profile.cacheVertices()) {
            List<ChunkMesh> farthest = new ArrayList<>(this.chunks.values());
            farthest.sort(Comparator.comparingDouble((ChunkMesh value) -> this.distance(value.chunk.getPos())).reversed());
            for (ChunkMesh candidate : farthest) {
                if (this.distance(candidate.chunk.getPos()) <= this.distance(mesh.chunk.getPos())) break;
                this.chunks.remove(candidate.chunk.getPos().pack());
                this.denied.add(candidate.chunk.getPos().pack());
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

    private void refreshDynamic(ClientLevel level, MunLightingProfile profile) {
        if (this.lastDynamicScan == level.getGameTime()) return;
        this.lastDynamicScan = level.getGameTime();
        this.dynamicCandidates.clear();
        // 独立于静态网格的构建进度，避免基地尚未入缓存时漏掉机器。
        int radius = (profile.dynamicDistance() + 15) / 16;
        ChunkPos center = ChunkPos.containing(BlockPos.containing(this.anchor));
        for (int z = center.z() - radius; z <= center.z() + radius; z++) {
            for (int x = center.x() - radius; x <= center.x() + radius; x++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity.isRemoved() || !entity.getType().isValid(entity.getBlockState())) continue;
                    if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity) == null) continue;
                    if (MunEntityShadows.distance(entity, this.anchor)
                        > profile.dynamicDistance() * profile.dynamicDistance()) continue;
                    this.dynamicCandidates.add(entity);
                }
            }
        }
    }

    private void render(MunSolarLighting solar, Vec3 center, MunLightingProfile profile, long gameTime) {
        Vec3 projectedCenter = solar.project(center);
        boolean select = false;
        for (int index = 0; index < profile.cascades(); index++) {
            Cascade cascade = this.cascades[index];
            cascade.span = (index == 2 ? this.farRadius : profile.radius(index)) * 2;
            cascade.resolution = Math.min(profile.resolution(index), RenderSystem.getDevice().getMaxTextureSize());
            boolean moved = cascade.projection.update(projectedCenter, this.renderOrigin, cascade.span, cascade.resolution);
            cascade.dirty = cascade.target == null || moved || cascade.geometryRevision != this.geometryRevision
                || cascade.solarRevision != solar.shadowRevision();
            select |= cascade.dirty;
        }
        if (select) this.selectMeshes(solar, profile);
        boolean dirty = false;
        for (int index = 0; index < profile.cascades(); index++) {
            Cascade cascade = this.cascades[index];
            cascade.dynamicDirty = this.hasDynamic();
            cascade.translucentDirty = profile.translucentShadows() && (cascade.translucentTarget == null
                || (cascade.translucentMeshes.isEmpty() ? !cascade.translucentEmpty
                    : cascade.dirty || cascade.translucentTick != gameTime));
            dirty |= cascade.dirty || cascade.dynamicDirty || cascade.translucentDirty;
        }
        if (!dirty) return;
        try (var outer = new MunShadowGlScope()) {
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            try (var ignored = new MunShadowGlScope()) {
                int sampler = GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 0);
                GL33C.glBindSampler(0, 0);
                try {
                    GlStateManager._enableDepthTest();
                    GlStateManager._depthFunc(GL11C.GL_LEQUAL);
                    GlStateManager._depthMask(true);
                    GlStateManager._disableBlend();
                    GlStateManager._disableCull();
                    GlStateManager._disableScissorTest();
                    GlStateManager._colorMask(15);
                    GL11C.glDepthRange(0, 1);
                    var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getTexture();
                    if (!(atlas instanceof GlTexture texture)) throw new IllegalStateException("Mun shadows require an OpenGL atlas");
                    GlStateManager._bindTexture(texture.glId());
                    for (int index = 0; index < profile.cascades(); index++) {
                        Cascade cascade = this.cascades[index];
                        if (!cascade.dirty && !cascade.dynamicDirty && !cascade.translucentDirty) continue;
                        if (cascade.target == null) cascade.target = new MunShadowTarget(cascade.resolution);
                        if (cascade.dynamicDirty && cascade.dynamicTarget == null) {
                            cascade.dynamicTarget = new MunShadowTarget(cascade.resolution);
                        }
                        this.shader.apply(solar, cascade.span, cascade.resolution, false);
                        if (cascade.dirty) {
                            cascade.target.bind();
                            for (ChunkMesh chunk : cascade.opaqueMeshes) {
                                if (chunk.mesh == null) continue;
                                Vec3 origin = new Vec3(chunk.chunk.getPos().getMinBlockX(), this.originY,
                                    chunk.chunk.getPos().getMinBlockZ());
                                this.draw(chunk.mesh, origin, cascade, this.shader);
                            }
                            if (profile.translucentShadows() && !cascade.translucentMeshes.isEmpty()) {
                                this.drawTranslucent(cascade, solar, true);
                                this.shader.apply(solar, cascade.span, cascade.resolution, false);
                            }
                            cascade.geometryRevision = this.geometryRevision;
                            cascade.solarRevision = solar.shadowRevision();
                        }
                        if (cascade.dynamicDirty && cascade.dynamicTarget != null) {
                            cascade.dynamicTarget.copyDepthFrom(cascade.target);
                            this.shader.origin(this.renderOrigin, cascade.projection.offset(this.renderOrigin));
                            this.entities.draw();
                        }
                        if (cascade.translucentDirty) {
                            this.drawTranslucent(cascade, solar, false);
                            cascade.translucentTick = gameTime;
                            cascade.translucentEmpty = cascade.translucentMeshes.isEmpty();
                        }
                    }
                } finally {
                    GL33C.glBindSampler(0, sampler);
                }
            }
        }
    }

    private void selectMeshes(MunSolarLighting solar, MunLightingProfile profile) {
        for (int index = 0; index < profile.cascades(); index++) {
            Cascade cascade = this.cascades[index];
            if (!cascade.dirty) continue;
            cascade.opaqueMeshes.clear();
            cascade.translucentMeshes.clear();
        }
        for (ChunkMesh chunk : this.chunks.values()) {
            Vec3 origin = new Vec3(chunk.chunk.getPos().getMinBlockX(), this.originY, chunk.chunk.getPos().getMinBlockZ());
            AABB opaque = chunk.mesh == null ? null : solar.projectBounds(chunk.mesh.bounds().move(origin));
            AABB translucent = chunk.translucent == null ? null : solar.projectBounds(chunk.translucent.bounds().move(origin));
            for (int index = 0; index < profile.cascades(); index++) {
                Cascade cascade = this.cascades[index];
                if (!cascade.dirty) continue;
                if (opaque != null && cascade.projection.intersects(opaque)) cascade.opaqueMeshes.add(chunk);
                if (translucent != null && cascade.projection.intersects(translucent)) cascade.translucentMeshes.add(chunk);
            }
        }
    }

    private void drawTranslucent(Cascade cascade, MunSolarLighting solar, boolean opaque) {
        if (!opaque) {
            if (cascade.translucentTarget == null) cascade.translucentTarget = new MunShadowTarget(cascade.resolution, true);
            cascade.translucentTarget.bind();
        }
        if (cascade.translucentMeshes.isEmpty()) return;
        this.translucentShader.apply(solar, cascade.span, cascade.resolution, opaque);
        for (ChunkMesh chunk : cascade.translucentMeshes) {
            if (chunk.translucent == null) continue;
            Vec3 origin = new Vec3(chunk.chunk.getPos().getMinBlockX(), this.originY, chunk.chunk.getPos().getMinBlockZ());
            this.draw(chunk.translucent, origin, cascade, this.translucentShader);
        }
    }

    private void draw(MunShadowMesh mesh, Vec3 origin, Cascade cascade, MunShadowProgram program) {
        program.origin(origin, cascade.projection.offset(origin));
        mesh.draw();
    }

    private boolean hasDynamic() {
        return !this.entities.isEmpty();
    }

    public void blockChanged(ClientLevel level, BlockPos pos, BlockState previous, BlockState state) {
        if (this.level != level) return;
        if (MunShadowMesh.fullCube(previous, level, pos) && MunShadowMesh.fullCube(state, level, pos)) return;
        this.chunkChanged(ChunkPos.containing(pos));
    }

    public void chunkChanged(ChunkPos pos) {
        if (this.level != null && this.contains(pos)) {
            this.meshesPending = true;
            this.lastChunkCheck = Long.MIN_VALUE;
            this.lastDynamicScan = Long.MIN_VALUE;
            this.invalidated.add(pos.pack());
            this.denied.remove(pos.pack());
        }
    }

    private boolean contains(ChunkPos pos) {
        int x = pos.getMinBlockX() - this.originX;
        int z = pos.getMinBlockZ() - this.originZ;
        return this.profile != null && x >= 0 && x < this.cacheRadius * 2 && z >= 0 && z < this.cacheRadius * 2;
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
        MunShadowTarget target = this.hasDynamic() ? cascade.dynamicTarget : cascade.target;
        return index < this.readyCascades && target != null ? target.texture() : 0;
    }

    int translucentTextureId(int index) {
        MunShadowTarget target = this.cascades[index].translucentTarget;
        return index < this.readyCascades && target != null ? target.transmission() : 0;
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
            if (cascade.translucentTarget != null) cascade.translucentTarget.close();
            cascade.target = null;
            cascade.dynamicTarget = null;
            cascade.translucentTarget = null;
            cascade.opaqueMeshes.clear();
            cascade.translucentMeshes.clear();
            cascade.translucentEmpty = true;
        }
        if (this.building != null) this.building.close();
        this.building = null;
        this.entities.close();
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
        this.cacheRadius = 0;
        this.farRadius = 0;
        this.readyCascades = 0;
        this.meshesPending = true;
        this.lastChunkCheck = Long.MIN_VALUE;
        this.lastDynamicScan = Long.MIN_VALUE;
        this.dynamicCandidates.clear();
        MunShadowMesh.clearCaches();
    }

    private static final class Cascade {
        private final MunShadowProjection projection = new MunShadowProjection();
        private final List<ChunkMesh> opaqueMeshes = new ArrayList<>();
        private final List<ChunkMesh> translucentMeshes = new ArrayList<>();
        private @Nullable MunShadowTarget target;
        private @Nullable MunShadowTarget dynamicTarget;
        private @Nullable MunShadowTarget translucentTarget;
        private float span;
        private int resolution;
        private long geometryRevision;
        private long solarRevision;
        private boolean dirty;
        private boolean dynamicDirty;
        private boolean translucentDirty;
        private boolean translucentEmpty = true;
        private long translucentTick;
    }

    private record ChunkMesh(LevelChunk chunk, @Nullable MunShadowMesh mesh, @Nullable MunShadowMesh translucent) implements AutoCloseable {
        int vertices() {
            return (this.mesh == null ? 0 : this.mesh.vertexCount()) + (this.translucent == null ? 0 : this.translucent.vertexCount());
        }

        @Override
        public void close() {
            if (this.mesh != null) this.mesh.close();
            if (this.translucent != null) this.translucent.close();
        }
    }

    private static final class BuildJob implements AutoCloseable {
        private final LevelChunk chunk;
        private final int height;
        private final int limit;
        private final boolean translucentShadows;
        private final int[] columns;
        private final List<BlockPos> models = new ArrayList<>();
        private final List<BlockPos> translucentModels = new ArrayList<>();
        private final List<BlockPos> fluids = new ArrayList<>();
        private int section;
        private int meshSlice;
        private int model;
        private int translucentModel;
        private int fluid;
        private @Nullable MunShadowMesh.Builder builder;
        private @Nullable MunShadowMesh.Builder translucentBuilder;

        private BuildJob(LevelChunk chunk, int height, int limit, boolean translucentShadows) {
            this.chunk = chunk;
            this.height = height;
            this.limit = limit;
            this.translucentShadows = translucentShadows;
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
            if (this.model < this.models.size() && !this.builder.full()) {
                BlockPos pos = this.models.get(this.model++);
                BlockState state = level.getBlockState(pos);
                Vec3 offset = state.getOffset(pos).add(pos.getX() & 15, pos.getY() - originY, pos.getZ() & 15);
                this.builder.model(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state), state, pos, offset,
                    state.getLightDampening() >= 15, level, false);
                return false;
            }
            if (!this.translucentShadows || this.builder.full()) return true;
            if (this.translucentBuilder == null) this.translucentBuilder = new MunShadowMesh.Builder(this.builder.remainingVertices());
            if (this.translucentBuilder.full()) return true;
            if (this.translucentModel >= this.translucentModels.size()) {
                if (this.fluid >= this.fluids.size()) return true;
                BlockPos pos = this.fluids.get(this.fluid++);
                this.translucentBuilder.fluid(level, level.getBlockState(pos), pos, originY);
                return false;
            }
            BlockPos pos = this.translucentModels.get(this.translucentModel++);
            BlockState state = level.getBlockState(pos);
            Vec3 offset = state.getOffset(pos).add(pos.getX() & 15, pos.getY() - originY, pos.getZ() & 15);
            this.translucentBuilder.model(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state),
                state, pos, offset, false, level, true);
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
                        if (this.translucentShadows) {
                            if (!state.getFluidState().isEmpty() && this.fluids.size() < this.limit / 64) this.fluids.add(pos.immutable());
                            if (MunShadowMesh.translucent(state, level, pos)) {
                                if (this.translucentModels.size() < this.limit / 64) this.translucentModels.add(pos.immutable());
                                continue;
                            }
                        }
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
            return new ChunkMesh(this.chunk, this.builder == null ? null : this.builder.finish(),
                this.translucentBuilder == null ? null : this.translucentBuilder.finish());
        }

        @Override
        public void close() {
            if (this.builder != null) this.builder.close();
            if (this.translucentBuilder != null) this.translucentBuilder.close();
        }
    }
}
