package dev.dubhe.anvilcraft.client.building;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintMultiblocks;
import dev.dubhe.anvilcraft.building.BuildingPlan;
import dev.dubhe.anvilcraft.network.BuildingRodResultPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 将服务端确认的放置与客户端世界更新配对，并在区块网格重建期间绘制放置过渡。 */
@NullMarked
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodPlacementAnimation {
    private static final Map<Long, Pending> pending = new HashMap<>();
    private static final List<Confirmed> confirmed = new ArrayList<>();
    private static final List<Batch> batches = new ArrayList<>();
    private static final Set<String> CONNECTIONS = Set.of("north", "south", "east", "west", "up", "down");
    private static volatile LongSet hidden = LongSet.of();
    private static @Nullable BuildingRodGhostFeatures features;
    private static long tick;

    private record Cell(BlockPos pos, BlockState expected, BlockState before, BoundingBox unit) {
    }

    private record Pending(ClientLevel level, Map<BlockPos, Cell> cells, long time) {
        boolean expired(long now) {
            return now - this.time >= 2_000_000_000L;
        }
    }

    private record Target(Cell cell, BlockState actual) {
        boolean matches(BlockState world) {
            if (world == this.cell.before || !world.is(this.actual.getBlock())) {
                return false;
            }
            if (world == this.actual) {
                return true;
            }
            // 方块更新可以把连续两次放置引起的邻接变化合并到同一个区块包。
            for (var property : this.actual.getProperties()) {
                if (!world.getValue(property).equals(this.actual.getValue(property))
                    && property != BlockStateProperties.STAIRS_SHAPE && property != BlockStateProperties.POWER
                    && !CONNECTIONS.contains(property.getName())) {
                    return false;
                }
            }
            return true;
        }
    }

    private record Confirmed(Map<BoundingBox, List<Target>> units, long deadline) {
    }

    private record Part(BlockPos pos, BlockState state, Vec3 pivot, int first, int terrainEnd, int end) {
    }

    private static final class Batch {
        final List<Part> parts = new ArrayList<>();
        final PlacementVertices vertices = new PlacementVertices();
        final BlockPos origin;
        long start;
        long release = -1;

        Batch(BlockPos origin) {
            this.origin = origin;
        }

        float progress(long now) {
            return Math.clamp((now - this.start) / 200_000_000f, 0f, 1f);
        }
    }

    private BuildingRodPlacementAnimation() {
    }

    static void remember(ClientLevel level, long seed, List<BuildingPlan.Cell> preview) {
        if (preview.isEmpty()) {
            return;
        }
        Map<BlockPos, BoundingBox> units = new HashMap<>();
        Map<BlockPos, Cell> cells = new HashMap<>();
        for (var cell : preview) {
            BlockPos pos = cell.pos().immutable();
            BlockPos core = BlueprintMultiblocks.core(pos, cell.state());
            var unit = units.computeIfAbsent(core, ignored -> new BoundingBox(pos));
            unit.encapsulate(pos);
            cells.put(pos, new Cell(pos, cell.state(), level.getBlockState(pos), unit));
        }
        pending.put(seed, new Pending(level, cells, System.nanoTime()));
    }

    static void confirm(long seed, List<BuildingRodResultPacket.Change> changes) {
        var request = pending.remove(seed);
        if (request == null || request.level != Minecraft.getInstance().level || request.expired(System.nanoTime())) {
            return;
        }
        Map<BoundingBox, List<Target>> units = new HashMap<>();
        for (var change : changes) {
            var cell = request.cells.get(change.pos());
            if (cell != null && change.state() != cell.before && change.state().is(cell.expected.getBlock())
                && !change.state().isAir() && (cell.expected.getFluidState().isEmpty()
                || cell.expected.getFluidState().getType().isSame(change.state().getFluidState().getType()))) {
                units.computeIfAbsent(cell.unit, ignored -> new ArrayList<>()).add(new Target(cell, change.state()));
            }
        }
        if (!units.isEmpty()) {
            confirmed.add(new Confirmed(units, tick + 3));
            advance(request.level, System.nanoTime());
        }
    }

    public static boolean hidesBlock(BlockPos pos) {
        var positions = hidden;
        return !positions.isEmpty() && positions.contains(pos.asLong());
    }

    public static boolean hidesBlock(int x, int y, int z) {
        var positions = hidden;
        return !positions.isEmpty() && positions.contains(BlockPos.asLong(x, y, z));
    }

    public static boolean hidesNeighbor(BlockPos pos, Direction face) {
        var positions = hidden;
        return !positions.isEmpty() && positions.contains(BlockPos.offset(pos.asLong(), face));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        tick++;
        long now = System.nanoTime();
        pending.values().removeIf(request -> request.expired(now));
        var level = Minecraft.getInstance().level;
        if (level != null) {
            advance(level, now);
        }
    }

    private static void advance(ClientLevel level, long now) {
        if (batches.isEmpty() && confirmed.isEmpty()) {
            return;
        }
        Set<BlockPos> dirty = new HashSet<>();
        for (var iterator = batches.iterator(); iterator.hasNext();) {
            var batch = iterator.next();
            batch.parts.removeIf(part -> {
                if (level.getBlockState(part.pos) == part.state) {
                    return false;
                }
                dirty.add(part.pos);
                return true;
            });
            if (batch.release < 0 && batch.progress(now) >= 1f) {
                batch.release = tick;
                batch.parts.forEach(part -> dirty.add(part.pos));
            }
            if (batch.parts.isEmpty() || batch.release >= 0 && tick - batch.release >= 2) {
                iterator.remove();
            }
        }
        for (var iterator = confirmed.iterator(); iterator.hasNext();) {
            var request = iterator.next();
            List<Target> ready = new ArrayList<>();
            request.units.values().removeIf(unit -> {
                if (tick < request.deadline && unit.stream().anyMatch(target -> !target.matches(level.getBlockState(target.cell.pos)))) {
                    return false;
                }
                for (var target : unit) {
                    var world = level.getBlockState(target.cell.pos);
                    if (target.matches(world)) {
                        ready.add(new Target(target.cell, world));
                    }
                }
                return true;
            });
            if (!ready.isEmpty()) {
                activate(level, ready, dirty);
            }
            if (request.units.isEmpty()) {
                iterator.remove();
            }
        }
        if (features != null && batches.stream().noneMatch(batch -> batch.release < 0)) {
            closeFeatures();
        }
        if (!dirty.isEmpty()) {
            var positions = new LongOpenHashSet();
            for (var batch : batches) {
                if (batch.release < 0) {
                    batch.parts.forEach(part -> positions.add(part.pos.asLong()));
                }
            }
            hidden = positions;
            refresh(level, dirty);
        }
    }

    private static void activate(ClientLevel level, List<Target> ready, Set<BlockPos> dirty) {
        var client = Minecraft.getInstance();
        var models = new ModelBlockRenderer(client.options.ambientOcclusion().get(), true, client.getBlockColors());
        var fluids = new FluidRenderer(client.getModelManager().getFluidStateModelSet());
        Map<BlockPos, BoundingBox> units = new HashMap<>();
        ready.forEach(target -> units.put(target.cell.pos, target.cell.unit));
        var view = new BlueprintRenderView(level, ready.getFirst().cell.pos, units, true);
        for (var previous : batches) {
            previous.parts.removeIf(part -> units.containsKey(part.pos));
        }
        var batch = new Batch(ready.getFirst().cell.pos);
        var extra = new PlacementVertices();
        for (var target : ready) {
            var pos = target.cell.pos;
            var pivot = AABB.of(target.cell.unit).getCenter();
            int first = batch.vertices.end();
            int terrainEnd = batch.vertices.bake(view, models, fluids, pos, target.actual, pivot, extra);
            var relative = pivot.subtract(batch.origin.getX(), batch.origin.getY(), batch.origin.getZ());
            batch.parts.add(new Part(pos, target.actual, relative, first, terrainEnd, batch.vertices.end()));
            dirty.add(pos);
        }
        batch.start = System.nanoTime();
        batches.add(batch);
    }

    private static void refresh(ClientLevel level, Iterable<BlockPos> positions) {
        var client = Minecraft.getInstance();
        var pipeline = CachedBlockEntityRenderingPipeline.getInstance();
        for (var pos : positions) {
            client.levelRenderer.setBlockDirty(pos, true);
            var entity = level.getBlockEntity(pos);
            if (entity != null && pipeline != null) {
                pipeline.update(entity, true);
            }
        }
    }

    static void render(ClientLevel level, RenderLevelStageEvent event) {
        final long now = System.nanoTime();
        if (batches.isEmpty()) {
            return;
        }
        var client = Minecraft.getInstance();
        var buffers = client.renderBuffers().bufferSource();
        var camera = event.getLevelRenderState().cameraRenderState;
        var pose = event.getPoseStack();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (var batch : batches) {
            boolean growing = batch.release < 0;
            float ease = easeOut(growing ? batch.progress(now) : 1f);
            float scale = BuildingRodRenderer.MATERIAL_SCALE + (1f - BuildingRodRenderer.MATERIAL_SCALE) * ease;
            int alpha = Math.round(BuildingRodRenderer.MATERIAL_ALPHA + (255 - BuildingRodRenderer.MATERIAL_ALPHA) * ease);
            var vertices = buffers.getBuffer(growing ? BuildingRodRenderTypes.BLOCK_GHOST : BuildingRodRenderTypes.PLACED);
            double originX = batch.origin.getX() - camera.pos.x;
            double originY = batch.origin.getY() - camera.pos.y;
            double originZ = batch.origin.getZ() - camera.pos.z;
            boolean hasEntities = false;
            for (var part : batch.parts) {
                double x = originX + part.pivot.x;
                double y = originY + part.pivot.y;
                double z = originZ + part.pivot.z;
                int end = growing ? part.end : part.terrainEnd;
                batch.vertices.draw(vertices, part.first, end, (float) x, (float) y, (float) z, scale, alpha);
                if (growing && part.state.hasBlockEntity()) {
                    var entity = level.getBlockEntity(part.pos);
                    if (entity != null) {
                        if (features == null) {
                            features = new BuildingRodGhostFeatures();
                        }
                        pose.pushPose();
                        try {
                            pose.translate(x, y, z);
                            pose.scale(scale, scale, scale);
                            pose.translate(part.pos.getX() - batch.origin.getX() - part.pivot.x,
                                part.pos.getY() - batch.origin.getY() - part.pivot.y,
                                part.pos.getZ() - batch.origin.getZ() - part.pivot.z);
                            features.submitPlaced(entity, pose, camera, partialTick);
                            hasEntities = true;
                        } finally {
                            pose.popPose();
                        }
                    }
                }
            }
            if (hasEntities && features != null) {
                features.drawPlaced(alpha);
            }
        }
        buffers.endBatch(BuildingRodRenderTypes.BLOCK_GHOST);
        buffers.endBatch(BuildingRodRenderTypes.PLACED);
    }

    static float easeOut(float progress) {
        float remaining = 1f - progress;
        return 1f - remaining * remaining * remaining;
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            clear();
            BuildingRodRenderer.clear();
        }
    }

    static void clear() {
        final var positions = hidden;
        hidden = LongSet.of();
        pending.clear();
        confirmed.clear();
        batches.clear();
        closeFeatures();
        var level = Minecraft.getInstance().level;
        if (level != null && !positions.isEmpty()) {
            refresh(level, positions.longStream().mapToObj(BlockPos::of).toList());
        }
    }

    private static void closeFeatures() {
        if (features != null) {
            features.close();
            features = null;
        }
    }
}
