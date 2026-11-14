package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.preview.ViewportCameraRig;
import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import dev.anvilcraft.lib.v2.rendering.preview.level.StructurePreviewPicker;
import dev.anvilcraft.lib.v2.rendering.preview.level.StructurePreviewRenderer;
import dev.anvilcraft.lib.v2.rendering.preview.level.StructureSandboxFactory;
import dev.dubhe.anvilcraft.building.BlueprintCapture;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.StructurePreviewTemplate;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/** 机器、磁盘提示和配方预览共用砧库的沙盒与原生结构渲染器。 */
@EventBusSubscriber(modid = "anvilcraft", value = Dist.CLIENT)
public record StructurePreviewScene(StructureTemplate template, SandboxRenderLevel level, BlockPos offset, AABB bounds) {
    private static final Map<LevelLike, CachedScene> CACHE = new WeakHashMap<>();
    @Nullable private static ClientLevel clientLevel;

    private record CachedScene(long revision, long animationFrame, int layer, boolean allLayers,
                               boolean blockEntities, StructurePreviewScene scene) {
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
        if (cached != null && cached.revision() == source.getRevision()
            && cached.animationFrame() == source.getAnimationFrame() && cached.layer() == source.getCurrentVisibleLayer()
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
        CACHE.put(source, new CachedScene(source.getRevision(), source.getAnimationFrame(), source.getCurrentVisibleLayer(),
            source.isAllLayersVisible(), blockEntities, scene));
        return scene;
    }

    public void submit(GuiGraphics graphics, Matrix4f transform, int x0, int y0, int x1, int y1,
                       float scale, VoxelShape range, float scanOpacity) {
        this.submit(graphics, transform, x0, y0, x1, y1, scale, range, scanOpacity, null);
    }

    public void submit(GuiGraphics graphics, Matrix4f transform, int x0, int y0, int x1, int y1,
                       float scale, VoxelShape range, float scanOpacity,
                       @Nullable BiConsumer<MultiBufferSource.BufferSource, PoseStack> additional) {
        BiConsumer<MultiBufferSource.BufferSource, PoseStack> draw = additional == null ? null : (collector, pose) -> {
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

    public static void render(SandboxRenderLevel level, ViewportCameraRig camera, GuiGraphics graphics,
                              int width, int height, float panX, float panY, VoxelShape range, float scanOpacity) {
        float scale = 10 * camera.getZoom();
        Matrix4f transform = new Matrix4f().translate(panX / scale, panY / scale, 0)
            .scale(1 / scale).mul(camera.buildViewMatrix());
        submitNative(level, graphics, transform, 0, 0, width, height, scale, range, scanOpacity, null);
    }

    private static final LinkedHashMap<Long, RenderTarget> TARGETS =
        new LinkedHashMap<>(8, 0.75f, true);

    @SubscribeEvent
    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {
        try (PreviewRenderState state = new PreviewRenderState()) {
            TARGETS.values().forEach(RenderTarget::destroyBuffers);
        }
        TARGETS.clear();
        CACHE.clear();
        clientLevel = null;
    }

    private static void submitNative(SandboxRenderLevel level, GuiGraphics graphics, Matrix4f transform,
                                     int x0, int y0, int x1, int y1, float scale, VoxelShape range, float scanOpacity,
                                     @Nullable BiConsumer<MultiBufferSource.BufferSource, PoseStack> additional) {
        var minecraft = Minecraft.getInstance();
        var parent = graphics.pose().last().pose();
        var first = parent.transformPosition(new Vector3f(x0, y0, 0));
        var last = parent.transformPosition(new Vector3f(x1, y1, 0));
        float parentScale = parent.getScale(new Vector3f()).x;
        int width = Math.max(1, Math.round(last.x - first.x));
        int height = Math.max(1, Math.round(last.y - first.y));
        int guiScale = (int) minecraft.getWindow().getGuiScale();
        int pixelsWide = width * guiScale;
        int pixelsHigh = height * guiScale;
        graphics.flush();
        RenderTarget target;
        try (PreviewRenderState state = new PreviewRenderState()) {
            long key = ((long) pixelsWide << 32) | pixelsHigh;
            target = TARGETS.get(key);
            if (target == null) {
                if (TARGETS.size() >= 8) {
                    Objects.requireNonNull(TARGETS.pollFirstEntry()).getValue().destroyBuffers();
                }
                target = new TextureTarget(pixelsWide, pixelsHigh, true, Minecraft.ON_OSX);
                TARGETS.put(key, target);
            }
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            ViewportCameraRig camera = new ViewportCameraRig() {
                @Override
                public Matrix4f buildViewMatrix() {
                    return new Matrix4f().scale(scale * parentScale).mul(transform);
                }
            };
            camera.configureViewport(width, height);
            StructurePreviewRenderer.getInstance().render(level, camera, graphics.bufferSource(),
                Integer.MIN_VALUE, Integer.MAX_VALUE, new StructurePreviewEffects(range, pixelsWide, pixelsHigh, additional));
        }
        StructurePreviewEffects.blit(graphics, target, x0, y0, x1, y1, scanOpacity);
    }

    public static @Nullable BlockPos pick(SandboxRenderLevel level, ViewportCameraRig camera, int width, int height,
                                         double mouseX, double mouseY, int minY, int maxY, float panX, float panY) {
        float scale = 10 * camera.getZoom();
        Matrix4f inverse = viewTransform(camera, panX, panY).invert();
        var start = inverse.transformPosition(new Vector3f((float) (mouseX - width / 2f) / scale,
            (float) (height / 2f - mouseY) / scale, 1000));
        var end = inverse.transformPosition(new Vector3f((float) (mouseX - width / 2f) / scale,
            (float) (height / 2f - mouseY) / scale, -1000));
        return StructurePreviewPicker.pick(level, new Vec3(start.x, start.y, start.z),
            new Vec3(end.x, end.y, end.z), minY, maxY);
    }

    public static Vector2f projectCenter(BlockPos pos, ViewportCameraRig camera, int width, int height, float panX, float panY) {
        float scale = 10 * camera.getZoom();
        var point = viewTransform(camera, panX, panY).transformPosition(new Vector3f(
            pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));
        return new Vector2f(width / 2f + point.x * scale, height / 2f - point.y * scale);
    }

    private static Matrix4f viewTransform(ViewportCameraRig camera, float panX, float panY) {
        float scale = 10 * camera.getZoom();
        return new Matrix4f().translate(panX / scale, panY / scale, 0).scale(1 / scale).mul(camera.buildViewMatrix());
    }
}
