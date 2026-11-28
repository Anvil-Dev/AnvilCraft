package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintBlockConfiguration;
import dev.dubhe.anvilcraft.building.BlueprintBlockEntities;
import dev.dubhe.anvilcraft.building.BlueprintMultiblocks;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.BuildingPlan;
import dev.dubhe.anvilcraft.building.EntityBuildAdapters;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodRenderer {
    @Nullable private static BuildingRodGhostMesh mesh;
    @Nullable private static StructureSnapshot built;
    private static Rotation rotation = Rotation.NONE;
    private static Mirror mirror = Mirror.NONE;
    private static int layer = -1;
    private static final int BLUEPRINT_ALPHA = 110;
    static final int MATERIAL_ALPHA = 166;
    static final float MATERIAL_SCALE = 0.64f;
    @Nullable private static BuildingRodGhostFeatures features;
    private static final BuildingRodAnimation ANIMATION = new BuildingRodAnimation();
    private static final List<BlockEntity> ENTITIES = new ArrayList<>();
    private static final List<Entity> PREVIEW_ENTITIES = new ArrayList<>();
    private static List<BuildingPlan.Cell> placementCells = List.of();
    @Nullable private static BoundingBox placementBounds;
    private static final Map<BlockPos, BoundingBox> PLACEMENT_UNITS = new HashMap<>();

    private BuildingRodRenderer() {
    }

    public static void clear() {
        BuildingRodRenderer.ANIMATION.clear();
        BuildingRodRenderer.clearMesh();
        if (features != null) {
            features.close();
            features = null;
        }
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        clear();
        BuildingRodRenderTypes.clearLayers();
        BuildingRodPlacementAnimation.clear();
        BuildingRodItemRenderer.clearModels();
    }

    static void turn(int quarterTurns) {
        BuildingRodRenderer.ANIMATION.turn(quarterTurns);
    }

    private static void clearMesh() {
        if (mesh != null) {
            mesh.close();
        }
        mesh = null;
        built = null;
        ENTITIES.clear();
        PREVIEW_ENTITIES.clear();
        placementCells = List.of();
        placementBounds = null;
        PLACEMENT_UNITS.clear();
        if (features != null) {
            features.clear();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (!IrisState.isShaderEnabled() && !IrisState.isRenderingShadows()) {
            renderProjection(event);
        }
    }

    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        if (!IrisState.isShaderEnabled() || IrisState.isRenderingShadows()) {
            return;
        }
        // 光影包可以丢弃顶点透明度；世界合成后仍保留主目标的地形深度。
        var matrices = RenderSystem.getModelViewStack();
        matrices.pushMatrix().set(event.getModelViewMatrix());
        try {
            renderProjection(event);
        } finally {
            matrices.popMatrix();
        }
    }

    private static void renderProjection(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mesh != null && !mesh.matchesPipeline()) {
            clearMesh();
        }
        BuildingRodPlacementAnimation.render(mc.level, event);
        StructureSnapshot snapshot = BuildingRodClient.snapshot;
        if (snapshot == null) {
            if (BuildingRodItem.isHeld(mc.player) && BuildingRodItem.isPlacementMaterial(BuildingRodItem.material(mc.player))) {
                renderMaterial(event, mc.level);
            }
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var bounds = BuildingRodClient.selectionBounds();
        if (bounds != null) {
            AABB box = AABB.of(bounds).move(-camera.x, -camera.y, -camera.z);
            var buffers = mc.renderBuffers().bufferSource();
            ShapeRenderer.renderShape(event.getPoseStack(), buffers.getBuffer(RenderTypes.lines()), Shapes.create(box), 0, 0, 0,
                BuildingRodClient.blueprintPlacements().isEmpty() ? 0xFFFF33CC : 0xFF33FFCC, 2);
            buffers.endBatch(RenderTypes.lines());
        }
        if (BuildingRodClient.traditional() && !BuildingRodTraditionalControls.isActive()
            || BuildingRodClient.target == null && !BuildingRodClient.locked) {
            return;
        }
        BlueprintPlacement placement = BuildingRodClient.placement;
        if (built != snapshot || rotation != placement.rotation() || mirror != placement.mirror() || layer != BuildingRodClient.layer) {
            build(snapshot, placement);
        }
        List<BlueprintPlacement> copies = BuildingRodClient.blueprintPlacements();
        for (BlueprintPlacement copy : copies) {
            renderCopy(event, snapshot, copy, copies.size() == 1);
        }
    }

    private static void renderMaterial(RenderLevelStageEvent event, ClientLevel level) {
        List<BuildingPlan.Cell> cells = BuildingRodClient.placementPreview();
        if (placementCells != cells && !placementCells.equals(cells)) {
            clearMesh();
            if (!cells.isEmpty()) {
                buildPlacement(level, cells);
            }
        }
        placementCells = cells;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        BoundingBox bounds = BuildingRodClient.selectionBounds();
        if (placementBounds != null) {
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(placementBounds.minX() - camera.x, placementBounds.minY() - camera.y, placementBounds.minZ() - camera.z);
            drawGhost(event, MATERIAL_ALPHA);
            pose.popPose();
            bounds = bounds == null ? placementBounds : BoundingBox.encapsulating(bounds, placementBounds);
        }
        if (bounds != null) {
            AABB box = AABB.of(bounds).move(-camera.x, -camera.y, -camera.z).inflate(0.002);
            drawMaterialBounds(event.getPoseStack(), box, cells.isEmpty() ? 0xFFFF5577 : 0xFFFFFFFF);
        }
    }

    private static void drawMaterialBounds(PoseStack pose, AABB box, int color) {
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderTypes.debugQuads());
        Vector3f min = box.getMinPosition().toVector3f();
        Vector3f max = box.getMaxPosition().toVector3f();
        for (FaceInfo face : FaceInfo.values()) {
            for (int i = 0; i < 4; i++) {
                vertices.addVertex(pose.last(), face.getVertexInfo(i).select(min, max)).setColor((color & 0xFFFFFF) | 0x1C000000);
            }
        }
        buffers.endBatch(RenderTypes.debugQuads());
        var lines = RenderTypes.lines();
        ShapeRenderer.renderShape(pose, buffers.getBuffer(lines), Shapes.create(box), 0, 0, 0, color, 2);
        if (IrisState.isShaderEnabled()) {
            buffers.endBatch(lines);
            return;
        }
        var target = BuildingRodRenderTypes.BLOCK_GHOST.outputTarget().getRenderTarget();
        var previousColor = RenderSystem.outputColorTextureOverride;
        var previousDepth = RenderSystem.outputDepthTextureOverride;
        // Fabulous 模式下复用虚影目标的颜色与深度缓冲，使白边读取刚写入的虚影深度。
        RenderSystem.outputColorTextureOverride = previousColor != null ? previousColor : target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = previousDepth != null ? previousDepth : target.getDepthTextureView();
        try {
            buffers.endBatch(lines);
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
        }
    }

    private static void renderCopy(RenderLevelStageEvent event, StructureSnapshot snapshot, BlueprintPlacement placement, boolean animate) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        Vec3 center = AABB.of(placement.bounds(snapshot.size())).getCenter();
        BuildingRodAnimation.Pose visual = !animate ? new BuildingRodAnimation.Pose(
            center.x,
            center.y,
            center.z,
            placement.yawDegrees(),
            placement.mirrorX(),
            placement.mirrorZ()
        ) : BuildingRodRenderer.ANIMATION.sample(
            center.x,
            center.y,
            center.z,
            placement.yawDegrees(),
            placement.mirrorX(),
            placement.mirrorZ(),
            System.nanoTime()
        );
        pose.pushPose();
        pose.translate(visual.x() - camera.x, visual.y() - camera.y, visual.z() - camera.z);
        // 先撤销已烘焙姿态，再套用插值姿态；旋转和镜像共用结构中心。
        pose.mulPose(Axis.YP.rotationDegrees(visual.yaw()));
        pose.scale(visual.mirrorX() * placement.mirrorX(), 1, visual.mirrorZ() * placement.mirrorZ());
        pose.mulPose(Axis.YP.rotationDegrees(-placement.yawDegrees()));
        pose.translate(placement.anchor().getX() - center.x, placement.anchor().getY() - center.y, placement.anchor().getZ() - center.z);
        Minecraft mc = Minecraft.getInstance();
        if (BuildingRodClient.traditional()) {
            var local = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror());
            var outlineBuffers = mc.renderBuffers().bufferSource();
            ShapeRenderer.renderShape(pose, outlineBuffers.getBuffer(RenderTypes.lines()),
                Shapes.create(AABB.of(local.bounds(snapshot.size()))), 0, 0, 0, 0xCC33E6FF, 2);
            outlineBuffers.endBatch(RenderTypes.lines());
        }
        drawGhost(event, BLUEPRINT_ALPHA);
        pose.popPose();
    }

    private static void drawGhost(RenderLevelStageEvent event, int alpha) {
        PoseStack pose = event.getPoseStack();
        if (mesh != null) {
            mesh.draw(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()));
        }
        if (!ENTITIES.isEmpty() || !PREVIEW_ENTITIES.isEmpty()) {
            if (features == null) {
                features = new BuildingRodGhostFeatures();
            }
            var camera = event.getLevelRenderState().cameraRenderState;
            features.render(ENTITIES, PREVIEW_ENTITIES, pose, camera, placementBounds == null ? Vec3.ZERO : camera.pos, alpha);
        }
    }

    private static void buildPlacement(ClientLevel level, List<BuildingPlan.Cell> cells) {
        BoundingBox bounds = new BoundingBox(cells.getFirst().pos());
        for (var cell : cells) {
            bounds.encapsulate(cell.pos());
        }
        placementBounds = bounds;
        BlockPos anchor = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        BlueprintRenderView view = new BlueprintRenderView(level, anchor, PLACEMENT_UNITS);
        Map<BlockPos, BoundingBox> groups = new HashMap<>();
        for (var cell : cells) {
            BlockPos pos = cell.pos();
            BlockPos core = BlueprintMultiblocks.core(pos, cell.state());
            BoundingBox unit = groups.computeIfAbsent(core, ignored -> new BoundingBox(pos));
            unit.encapsulate(pos);
            PLACEMENT_UNITS.put(pos, unit);
            BlockEntity entity = BlueprintBlockEntities.create(level, pos, cell.state(), cell.config().isEmpty() ? null : cell.config());
            if (entity != null) {
                ENTITIES.add(entity);
            }
            view.put(pos, cell.state(), entity);
        }
        bake(view, MATERIAL_ALPHA);
    }

    static void positionCell(PoseStack pose, BlockPos pos) {
        BoundingBox bounds = placementBounds;
        BoundingBox unit = PLACEMENT_UNITS.get(pos);
        if (bounds == null || unit == null) {
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        // 先转为网格局部坐标，远离世界原点时仍保留格内缩放的精度。
        Vec3 center = AABB.of(unit).getCenter();
        pose.translate(pos.getX() - bounds.minX() + (center.x - pos.getX()) * (1 - MATERIAL_SCALE),
            pos.getY() - bounds.minY() + (center.y - pos.getY()) * (1 - MATERIAL_SCALE),
            pos.getZ() - bounds.minZ() + (center.z - pos.getZ()) * (1 - MATERIAL_SCALE));
        pose.scale(MATERIAL_SCALE, MATERIAL_SCALE, MATERIAL_SCALE);
    }

    private static void build(StructureSnapshot snapshot, BlueprintPlacement placement) {
        BuildingRodRenderer.clearMesh();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        BlueprintRenderView view = new BlueprintRenderView(mc.level, placement.anchor(), Map.of());
        BlueprintPlacement local = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror());
        List<BlueprintMultiblocks.PlacedBlock> blocks;
        try {
            blocks = BlueprintMultiblocks.expand(snapshot, local, BuildingRodClient.layer);
        } catch (IllegalArgumentException exception) {
            built = snapshot;
            rotation = placement.rotation();
            mirror = placement.mirror();
            layer = BuildingRodClient.layer;
            return;
        }
        BlockPos sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        for (var entry : blocks) {
            BlockPos pos = entry.pos();
            var state = entry.state();
            var data = entry.nbt().map(nbt -> {
                var transformed = nbt.copy();
                BlueprintBlockConfiguration.transform(transformed, local, sourceOrigin);
                return transformed;
            }).orElse(null);
            BlockEntity entity = BlueprintBlockEntities.create(mc.level, pos, state, data);
            if (entity != null) {
                ENTITIES.add(entity);
            }
            view.put(pos, state, entity);
        }
        for (var entry : snapshot.entities()) {
            EntityBuildAdapters.create(BuildingEntityTransform.transform(entry, local), mc.level)
                .filter(entity -> !EntityBuildAdapters.isTransient(entity)).ifPresent(PREVIEW_ENTITIES::add);
        }
        bake(view, BLUEPRINT_ALPHA);
        built = snapshot;
        rotation = placement.rotation();
        mirror = placement.mirror();
        layer = BuildingRodClient.layer;
    }

    private static void bake(BlueprintRenderView view, int alpha) {
        Minecraft mc = Minecraft.getInstance();
        try (ByteBufferBuilder memory = new ByteBufferBuilder(2_097_152)) {
            BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, BuildingRodRenderTypes.BLOCK_GHOST.format());
            VertexConsumer vertices = new GhostConsumer(buffer, null, alpha);
            PoseStack pose = new PoseStack();
            ModelBlockRenderer renderer = new ModelBlockRenderer(true, true, mc.getBlockColors());
            FluidRenderer fluids = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
            for (BlockPos pos : view.positions()) {
                view.selectUnit(pos);
                var state = view.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                pose.pushPose();
                positionCell(pose, pos);
                renderer.tesselateBlock((x, y, z, quad, instance) -> {
                    pose.pushPose();
                    pose.translate(x, y, z);
                    vertices.putBakedQuad(pose.last(), quad, instance);
                    pose.popPose();
                }, 0, 0, 0, view, pos, state,
                    mc.getModelManager().getBlockStateModelSet().get(state), state.getSeed(pos));
                if (!state.getFluidState().isEmpty()) {
                    fluids.tesselate(view.shifted(pos), BlockPos.ZERO, ignored -> new GhostConsumer(buffer, pose.last(), alpha),
                        state, state.getFluidState());
                }
                pose.popPose();
            }
            MeshData data = buffer.build();
            if (data != null) {
                mesh = new BuildingRodGhostMesh(data);
            }
        }
    }

    record GhostConsumer(VertexConsumer delegate, @Nullable Pose transform, int opacity) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (this.transform == null) {
                this.delegate.addVertex(x, y, z);
            } else {
                this.delegate.addVertex(this.transform, x, y, z);
            }
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor((color & 0xFFFFFF) | (this.opacity << 24));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(red, green, blue, this.opacity);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(240, 240);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.delegate.setLineWidth(width);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
    }
}
