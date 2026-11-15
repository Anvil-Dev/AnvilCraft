package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.preview.StructurePreviewPipRenderer;
import dev.anvilcraft.lib.v2.rendering.preview.ViewportCameraRig;
import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import dev.anvilcraft.lib.v2.rendering.preview.level.StructureSandboxFactory;
import dev.dubhe.anvilcraft.building.BlueprintCapture;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.StructurePreviewTemplate;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/** 机器、磁盘提示和配方预览共用砧库的沙盒与原生结构渲染器。 */
public record StructurePreviewScene(StructureTemplate template, SandboxRenderLevel level, BlockPos offset, AABB bounds) {
    private static final Map<LevelLike, CachedScene> CACHE = new WeakHashMap<>();
    @Nullable private static ClientLevel clientLevel;

    private record CachedScene(long revision, int layer, boolean allLayers, boolean blockEntities, StructurePreviewScene scene) {
    }

    public static StructurePreviewScene of(LevelLike source) {
        return of(source, true);
    }

    public static StructurePreviewScene of(LevelLike source, boolean blockEntities) {
        ClientLevel current = Objects.requireNonNull(Minecraft.getInstance().level);
        if (clientLevel != current) {
            CACHE.clear();
            clientLevel = current;
        }
        CachedScene cached = CACHE.get(source);
        if (cached != null && cached.revision() == source.getRevision() && cached.layer() == source.getCurrentVisibleLayer()
            && cached.allLayers() == source.isAllLayersVisible() && cached.blockEntities() == blockEntities) {
            return cached.scene();
        }
        AABB bounds = source.getRenderBounds();
        var palette = new ArrayList<BlockState>();
        var blocks = new ArrayList<StructureSnapshot.BlockEntry>();
        for (BlockPos cursor : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
            BlockPos.containing(Math.ceil(bounds.maxX) - 1, Math.ceil(bounds.maxY) - 1, Math.ceil(bounds.maxZ) - 1))) {
            BlockState state = source.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            int index = palette.indexOf(state);
            if (index < 0) {
                index = palette.size();
                palette.add(state);
            }
            var original = source.getBlockEntityData(cursor);
            var data = original == null
                ? Optional.ofNullable(source.getBlockEntity(cursor)).map(entity -> entity.saveWithFullMetadata(current.registryAccess()))
                : Optional.of(original);
            blocks.add(new StructureSnapshot.BlockEntry(cursor.immutable(), index, data));
        }
        var entities = new ArrayList<StructureSnapshot.EntityEntry>();
        for (var entity : source.getEntities()) {
            if (!source.isAllLayersVisible() && (entity.getBoundingBox().maxY <= source.getCurrentVisibleLayer()
                || entity.getBoundingBox().minY >= source.getCurrentVisibleLayer() + 1)) {
                continue;
            }
            var data = BlueprintCapture.captureEntity(entity);
            if (data != null) {
                entities.add(new StructureSnapshot.EntityEntry(entity.position(), entity.blockPosition(), data));
            }
        }
        Vec3i size = new Vec3i((int) (Math.ceil(bounds.maxX) - Math.floor(bounds.minX)),
            (int) (Math.ceil(bounds.maxY) - Math.floor(bounds.minY)), (int) (Math.ceil(bounds.maxZ) - Math.floor(bounds.minZ)));
        var snapshot = new StructureSnapshot(size, palette, blocks, entities);
        var prepared = StructurePreviewTemplate.create(snapshot,
            new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE), bounds, null, null, current.registryAccess());
        BlockPos origin = StructureSandboxFactory.centeredPlacement(prepared.template());
        var scene = new StructurePreviewScene(prepared.template(),
            Objects.requireNonNull(StructureSandboxFactory.create(current, prepared.template(), origin)),
            origin.subtract(prepared.minimum()), bounds);
        if (!blockEntities) {
            scene.level().getFilledBlocks().forEach(scene.level()::removeBlockEntity);
        }
        CACHE.put(source, new CachedScene(source.getRevision(), source.getCurrentVisibleLayer(),
            source.isAllLayersVisible(), blockEntities, scene));
        return scene;
    }

    public void submit(GuiGraphicsExtractor graphics, Matrix4f transform, int x0, int y0, int x1, int y1,
                       float scale, VoxelShape range, float scanOpacity) {
        this.submit(graphics, transform, x0, y0, x1, y1, scale, range, scanOpacity, null);
    }

    public void submit(GuiGraphicsExtractor graphics, Matrix4f transform, int x0, int y0, int x1, int y1,
                       float scale, VoxelShape range, float scanOpacity,
                       @Nullable BiConsumer<SubmitNodeCollector, PoseStack> additional) {
        BiConsumer<SubmitNodeCollector, PoseStack> draw = additional == null ? null : (collector, pose) -> {
            pose.pushPose();
            try {
                pose.translate(this.offset.getX(), this.offset.getY(), this.offset.getZ());
                additional.accept(collector, pose);
            } finally {
                pose.popPose();
            }
        };
        submitNative(this.level, graphics, new Matrix4f(transform).translate(-this.offset.getX(), -this.offset.getY(), -this.offset.getZ()),
            x0, y0, x1, y1, scale, range.move(this.offset.getX(), this.offset.getY(), this.offset.getZ()), scanOpacity, draw);
    }

    public static void render(SandboxRenderLevel level, ViewportCameraRig camera, GuiGraphicsExtractor graphics,
                              int width, int height, float panX, float panY, VoxelShape range, float scanOpacity) {
        float scale = 10 * camera.getZoom();
        Matrix4f transform = new Matrix4f().translate(panX / scale, panY / scale, 0)
            .scale(1 / scale).mul(camera.buildViewMatrix());
        submitNative(level, graphics, transform, 0, 0, width, height, scale, range, scanOpacity, null);
    }

    private static void submitNative(SandboxRenderLevel level, GuiGraphicsExtractor graphics, Matrix4f transform,
                               int x0, int y0, int x1, int y1, float scale, VoxelShape range, float scanOpacity,
                               @Nullable BiConsumer<SubmitNodeCollector, PoseStack> additional) {
        float parentScale = Math.max((float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11()));
        Vector2f first = graphics.pose().transformPosition(x0, y0, new Vector2f());
        Vector2f last = graphics.pose().transformPosition(x1, y1, new Vector2f());
        var state = new StructurePreviewPipRenderer.State(level, Integer.MIN_VALUE, Integer.MAX_VALUE, transform,
            Math.round(first.x), Math.round(first.y), Math.round(last.x), Math.round(last.y),
            scale * parentScale, graphics.peekScissorStack(), new StructurePreviewEffects(range, scanOpacity, additional));
        if (state.bounds() != null) {
            graphics.submitPictureInPictureRenderState(state);
        }
    }
}
