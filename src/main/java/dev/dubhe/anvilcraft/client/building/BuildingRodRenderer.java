package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.renderer.projection.ProjectionRenderer;
import dev.anvilcraft.lib.v2.renderer.projection.ProjectionScene;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.building.BlueprintBlockConfiguration;
import dev.dubhe.anvilcraft.building.BlueprintBlockEntities;
import dev.dubhe.anvilcraft.building.BlueprintMultiblocks;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.BuildingRodService;
import dev.dubhe.anvilcraft.building.EntityBuildAdapters;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.client.event.LargeBlockPlacePreviewEventListener;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.biome.Biomes;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodRenderer {
    private static final ProjectionRenderer PROJECTION = new ProjectionRenderer();
    @Nullable private static StructureSnapshot built;
    private static Rotation rotation = Rotation.NONE;
    private static Mirror mirror = Mirror.NONE;
    private static int layer = -1;
    private static int meshAlpha = 110;
    private static final BuildingRodAnimation ANIMATION = new BuildingRodAnimation();
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
        PROJECTION.close();
        built = null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var bounds = BuildingRodClient.selectionBounds();
        if (bounds != null) {
            AABB box = AABB.of(bounds).move(-camera.x, -camera.y, -camera.z);
            boolean valid = BuildingRodClient.snapshot == null
                ? (long) bounds.getXSpan() * bounds.getYSpan() * bounds.getZSpan() <= BuildingRodService.MAX_BLOCKS
                : !BuildingRodClient.blueprintPlacements().isEmpty();
            var buffers = mc.renderBuffers().bufferSource();
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, valid ? 0.2f : 1, valid ? 1 : 0.2f, 0.8f, 1);
            buffers.endBatch(RenderType.lines());
        }
        StructureSnapshot snapshot = BuildingRodClient.snapshot;
        boolean materialPreview = snapshot == null && BuildingRodItem.isHeld(mc.player);
        if (!materialPreview && BuildingRodClient.traditional() && !BuildingRodTraditionalControls.isActive()) return;
        BlueprintPlacement placement = BuildingRodClient.placement;
        if (materialPreview) {
            var material = BuildingRodItem.material(mc.player);
            if (!material.is(ModItems.FILTER) && (!(material.getItem() instanceof BlockItem item)
                || !LargeBlockPlacePreviewEventListener.isPreviewable(item.getBlock()))) {
                return;
            }
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
        if (snapshot == null || (BuildingRodClient.target == null && !BuildingRodClient.locked)) return;
        int alpha = materialPreview ? (int) Math.round(255 * AnvilCraft.CLIENT_CONFIG.multiPartPreviewGhostOpacity) : 110;
        if (!PROJECTION.isValid() || built != snapshot || rotation != placement.rotation() || mirror != placement.mirror()
            || layer != BuildingRodClient.layer || meshAlpha != alpha) {
            build(snapshot, placement, alpha);
        }
        List<BlueprintPlacement> copies = materialPreview ? List.of(placement) : BuildingRodClient.blueprintPlacements();
        for (BlueprintPlacement copy : copies) {
            renderCopy(event, snapshot, copy, materialPreview, !materialPreview && copies.size() == 1);
        }
    }

    private static void renderCopy(
        RenderLevelStageEvent event, StructureSnapshot snapshot, BlueprintPlacement placement,
        boolean materialPreview, boolean animate
    ) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        Vec3 center = AABB.of(placement.bounds(snapshot.size())).getCenter();
        BuildingRodAnimation.Pose visual = !animate ? new BuildingRodAnimation.Pose(center.x, center.y, center.z, placement.yawDegrees(),
                placement.mirrorX(), placement.mirrorZ())
            : ANIMATION.sample(center.x, center.y, center.z, placement.yawDegrees(),
                placement.mirrorX(), placement.mirrorZ(), System.nanoTime());
        pose.pushPose();
        pose.translate(visual.x() - camera.x, visual.y() - camera.y, visual.z() - camera.z);
        // 先撤销已烘焙姿态，再套用插值姿态；旋转和镜像共用结构中心。
        pose.mulPose(Axis.YP.rotationDegrees(visual.yaw()));
        pose.scale(visual.mirrorX() * placement.mirrorX(), 1, visual.mirrorZ() * placement.mirrorZ());
        pose.mulPose(Axis.YP.rotationDegrees(-placement.yawDegrees()));
        pose.translate(placement.anchor().getX() - center.x, placement.anchor().getY() - center.y, placement.anchor().getZ() - center.z);
        Minecraft mc = Minecraft.getInstance();
        if (!materialPreview && BuildingRodClient.traditional()) {
            var local = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror());
            var outlineBuffers = mc.renderBuffers().bufferSource();
            LevelRenderer.renderLineBox(pose, outlineBuffers.getBuffer(RenderType.lines()),
                AABB.of(local.bounds(snapshot.size())), 0.2f, 0.9f, 1, 0.8f);
            outlineBuffers.endBatch(RenderType.lines());
        }
        PROJECTION.render(pose, event.getProjectionMatrix(), mc.renderBuffers().bufferSource());
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
        var plains = mc.level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS).value();
        ProjectionScene view = new ProjectionScene(mc.level, placement.anchor(),
            (pos, resolver) -> resolver.getColor(plains, placement.anchor().getX(), placement.anchor().getZ()));
        BlueprintPlacement local = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror());
        List<BlueprintMultiblocks.PlacedBlock> blocks;
        try {
            blocks = BlueprintMultiblocks.expand(snapshot, local, BuildingRodClient.layer);
        } catch (IllegalArgumentException exception) {
            PROJECTION.rebuild(view, alpha);
            built = snapshot;
            rotation = placement.rotation();
            mirror = placement.mirror();
            layer = BuildingRodClient.layer;
            meshAlpha = alpha;
            return;
        }
        BlockPos sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        for (var entry : blocks) {
            BlockPos pos = entry.pos();
            var state = entry.state();
            var data = entry.nbt().map(nbt -> {
                var transformed = nbt.copy();
                BlueprintBlockConfiguration.transform(transformed, local, sourceOrigin, snapshot);
                return transformed;
            }).orElse(null);
            BlockEntity entity = BlueprintBlockEntities.create(mc.level, pos, state, data);
            view.put(pos, state, entity);
        }
        for (var entry : snapshot.entities()) {
            EntityBuildAdapters.create(BuildingEntityTransform.transform(entry, local), mc.level)
                .filter(entity -> !EntityBuildAdapters.isTransient(entity)).ifPresent(view::addEntity);
        }
        PROJECTION.rebuild(view, alpha);
        built = snapshot;
        rotation = placement.rotation();
        mirror = placement.mirror();
        layer = BuildingRodClient.layer;
        meshAlpha = alpha;
    }
}
