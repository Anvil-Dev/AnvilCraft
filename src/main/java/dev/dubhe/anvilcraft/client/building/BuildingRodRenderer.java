package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.BuildingRodService;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.client.event.LargeBlockPlacePreviewEventListener;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodRenderer {
    @Nullable private static VertexBuffer mesh;
    @Nullable private static StructureSnapshot built;
    private static Rotation rotation = Rotation.NONE;
    private static Mirror mirror = Mirror.NONE;
    private static int layer = -1;
    private static int meshAlpha = 110;
    private static final BuildingRodAnimation ANIMATION = new BuildingRodAnimation();
    private static final List<BlockEntity> ENTITIES = new ArrayList<>();
    private static final List<Entity> PREVIEW_ENTITIES = new ArrayList<>();
    private static List<BuildingRodService.Cell> placementCells = List.of();
    @Nullable private static StructureSnapshot placementSnapshot;
    private static BlockPos placementAnchor = BlockPos.ZERO;

    private BuildingRodRenderer() {
    }

    public static void clear() {
        ANIMATION.clear();
        clearMesh();
        placementCells = List.of();
        placementSnapshot = null;
    }

    static void turn(int quarterTurns) {
        ANIMATION.turn(quarterTurns);
    }

    private static void clearMesh() {
        if (mesh != null) mesh.close();
        mesh = null;
        built = null;
        ENTITIES.clear();
        PREVIEW_ENTITIES.clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        BlockPos first = BuildingRodClient.first;
        BlockPos target = BuildingRodClient.target;
        if (first != null && target != null) {
            AABB box = AABB.encapsulatingFullBlocks(first, target).move(-camera.x, -camera.y, -camera.z);
            boolean valid = BuildingRodService.volume(first, target) <= BuildingRodService.MAX_BLOCKS;
            var buffers = mc.renderBuffers().bufferSource();
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, valid ? 0.2f : 1, valid ? 1 : 0.2f, 0.8f, 1);
            buffers.endBatch(RenderType.lines());
        }
        StructureSnapshot snapshot = BuildingRodClient.snapshot;
        boolean materialPreview = snapshot == null && mc.player.getMainHandItem().is(ModItems.BUILDING_ROD);
        if (!materialPreview && BuildingRodClient.traditional() && !BuildingRodTraditionalControls.isActive()) return;
        BlueprintPlacement placement = BuildingRodClient.placement;
        if (materialPreview) {
            if (!(mc.player.getOffhandItem().getItem() instanceof BlockItem item)
                || !LargeBlockPlacePreviewEventListener.isPreviewable(item.getBlock())) return;
            List<BuildingRodService.Cell> cells = BuildingRodClient.placementPreview();
            if (cells.isEmpty() || AnvilCraft.CLIENT_CONFIG.multiPartPreviewMode == AnvilCraftClientConfig.MultiPartPreviewMode.OFF) return;
            if (AnvilCraft.CLIENT_CONFIG.multiPartPreviewMode == AnvilCraftClientConfig.MultiPartPreviewMode.OUTLINE
                && renderPlacementOutline(event, cells)) {
                return;
            }
            preparePlacement(cells);
            snapshot = placementSnapshot;
            placement = new BlueprintPlacement(placementAnchor, Rotation.NONE, Mirror.NONE);
        }
        if (snapshot == null || (target == null && !BuildingRodClient.locked)) return;
        int alpha = materialPreview ? (int) Math.round(255 * AnvilCraft.CLIENT_CONFIG.multiPartPreviewGhostOpacity) : 110;
        if (built != snapshot || rotation != placement.rotation() || mirror != placement.mirror()
            || layer != BuildingRodClient.layer || meshAlpha != alpha) {
            build(snapshot, placement, alpha);
        }
        Vec3 center = AABB.of(placement.bounds(snapshot.size())).getCenter();
        BuildingRodAnimation.Pose visual = materialPreview ? new BuildingRodAnimation.Pose(center.x, center.y, center.z, 0, 1, 1)
            : ANIMATION.sample(center.x, center.y, center.z, placement.yawDegrees(),
                placement.mirrorX(), placement.mirrorZ(), System.nanoTime());
        pose.pushPose();
        pose.translate(visual.x() - camera.x, visual.y() - camera.y, visual.z() - camera.z);
        // 先撤销已烘焙姿态，再套用插值姿态；旋转和镜像共用结构中心。
        pose.mulPose(Axis.YP.rotationDegrees(visual.yaw()));
        pose.scale(visual.mirrorX() * placement.mirrorX(), 1, visual.mirrorZ() * placement.mirrorZ());
        pose.mulPose(Axis.YP.rotationDegrees(-placement.yawDegrees()));
        pose.translate(placement.anchor().getX() - center.x, placement.anchor().getY() - center.y, placement.anchor().getZ() - center.z);
        if (!materialPreview && BuildingRodClient.traditional()) {
            var local = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror());
            var outlineBuffers = mc.renderBuffers().bufferSource();
            LevelRenderer.renderLineBox(pose, outlineBuffers.getBuffer(RenderType.lines()),
                AABB.of(local.bounds(snapshot.size())), 0.2f, 0.9f, 1, 0.8f);
            outlineBuffers.endBatch(RenderType.lines());
        }
        if (mesh != null) {
            RenderType type = BuildingRodRenderTypes.ghost(RenderType.translucent());
            type.setupRenderState();
            mesh.bind();
            // 缓存网格直接绘制，需要显式合入相机视图矩阵，与实体缓冲渲染保持一致。
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
            mesh.drawWithShader(modelView, event.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            type.clearRenderState();
        }
        var buffers = mc.renderBuffers().bufferSource();
        MultiBufferSource ghostBuffers = type -> new GhostConsumer(
            buffers.getBuffer(BuildingRodRenderTypes.ghost(type)), BlockPos.ZERO, alpha);
        for (BlockEntity entity : ENTITIES) {
            pose.pushPose();
            BlockPos pos = entity.getBlockPos();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            mc.getBlockEntityRenderDispatcher().renderItem(entity, pose, ghostBuffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        for (Entity entity : PREVIEW_ENTITIES) {
            mc.getEntityRenderDispatcher().render(entity, entity.getX(), entity.getY(), entity.getZ(),
                entity.getYRot(), 0, pose, ghostBuffers, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
        pose.popPose();
    }

    private static void preparePlacement(List<BuildingRodService.Cell> cells) {
        if (placementCells.equals(cells)) return;
        placementCells = cells;
        int minX = cells.stream().mapToInt(cell -> cell.pos().getX()).min().orElse(0);
        int minY = cells.stream().mapToInt(cell -> cell.pos().getY()).min().orElse(0);
        int minZ = cells.stream().mapToInt(cell -> cell.pos().getZ()).min().orElse(0);
        int maxX = cells.stream().mapToInt(cell -> cell.pos().getX()).max().orElse(0);
        int maxY = cells.stream().mapToInt(cell -> cell.pos().getY()).max().orElse(0);
        int maxZ = cells.stream().mapToInt(cell -> cell.pos().getZ()).max().orElse(0);
        placementAnchor = new BlockPos(minX, minY, minZ);
        Map<BlockState, Integer> palette = new LinkedHashMap<>();
        List<StructureSnapshot.BlockEntry> entries = new ArrayList<>();
        for (var cell : cells) {
            int index = palette.computeIfAbsent(cell.state(), state -> palette.size());
            entries.add(new StructureSnapshot.BlockEntry(cell.pos().subtract(placementAnchor), index,
                cell.config().isEmpty() ? Optional.empty() : Optional.of(cell.config())));
        }
        placementSnapshot = new StructureSnapshot(new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1),
            new ArrayList<>(palette.keySet()), entries, List.of());
    }

    private static boolean renderPlacementOutline(RenderLevelStageEvent event, List<BuildingRodService.Cell> cells) {
        Map<BuildingRodService.Cell, List<SelectionPart>> outlines = new LinkedHashMap<>();
        for (var cell : cells) {
            if (cell.state().getBlock() instanceof AbstractMultiPartBlock<?> multipart && !multipart.isMainPart(cell.state())) continue;
            List<SelectionPart> parts = new ArrayList<>(ModelBlockSelection.multipartOutline(cell.state()));
            if (cell.state().hasBlockEntity()) parts.addAll(ModelBlockSelection.previewBerParts(cell.state(), cell.pos()));
            // 与普通放置预览一致：没有可用模型描边时回退到虚影，不以包围盒冒充模型。
            if (parts.isEmpty()) return false;
            outlines.put(cell, parts);
        }
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        for (var entry : outlines.entrySet()) {
            BlockPos pos = entry.getKey().pos();
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            for (SelectionPart part : entry.getValue()) {
                pose.pushPose();
                part.apply(pose);
                OutlineRenderer.render(pose, vertices, CubeSelection.outlines().get(part.geometry()), 1, 1, 1,
                    (float) AnvilCraft.CLIENT_CONFIG.multiPartPreviewOutlineOpacity);
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch(RenderType.lines());
        return true;
    }

    private static void build(StructureSnapshot snapshot, BlueprintPlacement placement, int alpha) {
        clearMesh();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlueprintRenderView view = new BlueprintRenderView(mc.level, placement.anchor());
        BlueprintPlacement local = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror());
        for (var entry : snapshot.blocks()) {
            if (BuildingRodClient.layer >= 0 && entry.pos().getY() != BuildingRodClient.layer) continue;
            BlockPos pos = local.worldOf(entry.pos());
            var state = local.stateOf(snapshot.stateOf(entry));
            BlockEntity entity = state.getBlock() instanceof EntityBlock block ? block.newBlockEntity(pos, state) : null;
            if (entity != null) {
                entry.nbt().ifPresent(nbt -> entity.loadWithComponents(nbt, mc.level.registryAccess()));
                entity.setLevel(mc.level);
                ENTITIES.add(entity);
            }
            view.put(pos, state, entity);
        }
        for (var entry : snapshot.entities()) {
            EntityType.create(BuildingEntityTransform.transform(entry, local), mc.level).ifPresent(PREVIEW_ENTITIES::add);
        }
        try (ByteBufferBuilder memory = new ByteBufferBuilder(2_097_152)) {
            BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            VertexConsumer vertices = new GhostConsumer(buffer, BlockPos.ZERO, alpha);
            PoseStack pose = new PoseStack();
            RandomSource random = RandomSource.create();
            for (var entry : snapshot.blocks()) {
                if (BuildingRodClient.layer >= 0 && entry.pos().getY() != BuildingRodClient.layer) continue;
                BlockPos pos = local.worldOf(entry.pos());
                var state = view.realState(pos);
                if (state.isAir()) continue;
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                ModelData data = view.getModelData(pos);
                var model = mc.getBlockRenderer().getBlockModel(state);
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    mc.getBlockRenderer().renderBatched(state, pos, view, pose, vertices, true, random, data, type);
                }
                pose.popPose();
                if (!state.getFluidState().isEmpty()) {
                    mc.getBlockRenderer().renderLiquid(BlockPos.ZERO, view.shifted(pos), new GhostConsumer(buffer, pos, alpha),
                        state, state.getFluidState());
                }
            }
            MeshData data = buffer.build();
            if (data != null) {
                mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
                mesh.bind();
                mesh.upload(data);
                VertexBuffer.unbind();
            }
        }
        built = snapshot;
        rotation = placement.rotation();
        mirror = placement.mirror();
        layer = BuildingRodClient.layer;
        meshAlpha = alpha;
    }

    private record GhostConsumer(VertexConsumer delegate, BlockPos offset, int opacity) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x + this.offset.getX(), y + this.offset.getY(), z + this.offset.getZ());
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
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
    }
}
