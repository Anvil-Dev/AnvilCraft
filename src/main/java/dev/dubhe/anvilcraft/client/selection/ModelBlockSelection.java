package dev.dubhe.anvilcraft.client.selection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelection;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.mixin.accessor.ModelBakeryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ModelBlockSelection {
    private static long frame;
    private static Snapshot snapshot = new Snapshot(Map.of(), Map.of(), Map.of());
    private static final Map<BlockPos, List<SelectionPart>> DYNAMIC = new HashMap<>();
    /** 放置预览用的方块实体模型（按状态缓存，预览位置上还没有真实实体）。 */
    private static final Map<BlockState, List<SelectionPart>> PREVIEW_BER_PARTS = new IdentityHashMap<>();
    private static final Map<BlockState, List<ModelPlacement>> PREVIEW_BER_MODELS = new IdentityHashMap<>();
    private static final Cache<VoxelShape, SelectionPart> FALLBACK = CacheBuilder.newBuilder().weakKeys()
        .maximumWeight(4 * 1024 * 1024)
        .weigher((VoxelShape key, SelectionPart value) -> (int) value.geometry().estimatedBytes() + 512)
        .build();

    /** 一个离散模型及其在方块内的位姿，供预览按贴图渲染（GHOST 模式）。 */
    public record ModelPlacement(ModelResourceLocation model, Matrix4f pose) {
    }

    private ModelBlockSelection() {
    }

    public static void reload(ModelEvent.BakingCompleted event) {
        ModelSelectionBlacklist.reload(Minecraft.getInstance().getResourceManager());
        ModelBakeryAccessor bakery = (ModelBakeryAccessor) event.getModelBakery();
        snapshot = new ModelSelectionBakery(bakery.getUnbakedCache()).bake(bakery.getTopLevelModels());
        DYNAMIC.clear();
        PREVIEW_BER_PARTS.clear();
        PREVIEW_BER_MODELS.clear();
        FALLBACK.invalidateAll();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!AnvilCraft.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            AABB bounds = null;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelSelection selection = snapshot.states().get(state);
                if (selection != null) bounds = bounds == null ? selection.bounds() : bounds.minmax(selection.bounds());
            }
            if (bounds == null) continue;
            if (block instanceof EntityBlock) bounds = new AABB(-2, -2, -2, 3, 3, 3);
            AABB checked = block.defaultBlockState().hasOffsetFunction() ? bounds.inflate(0.5) : bounds;
            if (CubeSelection.supportedBounds(checked)) {
                CubeSelection.registerDynamic(block, bounds, false, ModelBlockSelection::parts);
            }
        }
    }

    @SubscribeEvent
    public static void beginFrame(RenderFrameEvent.Pre event) {
        frame++;
        DYNAMIC.clear();
    }

    public static long frame() {
        return frame;
    }

    /** Returns the whole-multipart outline parts for a placement/main-part state, if prepared. */
    public static List<SelectionPart> multipartOutline(BlockState state) {
        List<SelectionPart> outline = snapshot.outlines().get(state);
        return outline == null ? List.of() : outline;
    }

    /**
     * 放置预览用的方块实体模型几何（描边模式）。
     *
     * <p>方块模型与实体模型是两套模型（如智能方块放置器 = 底座方块模型 + 机械臂实体模型），
     * 实体部分不在 {@code snapshot.outlines()} 中。预览位置上还没有方块实体，故按放置状态
     * 临时构造一个（与 {@code RenderSupport} 的做法一致）取其静止姿态。</p>
     */
    public static List<SelectionPart> previewBerParts(BlockState state) {
        return ModelBlockSelection.PREVIEW_BER_PARTS.computeIfAbsent(state, key -> {
            BlockEntity entity = ModelBlockSelection.previewEntity(key);
            return entity == null ? List.of() : ModelBlockSelection.rendererParts(entity, 0.0F, true);
        });
    }

    /**
     * 放置预览用的方块实体模型与位姿（虚影模式，按贴图渲染）。
     *
     * @see #previewBerParts(BlockState)
     */
    public static List<ModelPlacement> previewBerModels(BlockState state) {
        return ModelBlockSelection.PREVIEW_BER_MODELS.computeIfAbsent(state, key -> {
            BlockEntity entity = ModelBlockSelection.previewEntity(key);
            if (entity == null) return List.of();
            BlockEntityRenderer<?> renderer = Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .getRenderer(entity);
            if (!(renderer instanceof ModelSelectionRenderer<?> selectionRenderer)) return List.of();
            List<ModelPlacement> result = new ArrayList<>();
            ModelBlockSelection.collectPreviewModels(selectionRenderer, entity, result);
            return List.copyOf(result);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void collectPreviewModels(
        ModelSelectionRenderer<T> renderer,
        BlockEntity entity,
        List<ModelPlacement> output
    ) {
        renderer.collectPreviewModels((T) entity, 0.0F, new PoseStack(), (model, pose) ->
            output.add(new ModelPlacement(model, new Matrix4f(pose.last().pose()))));
    }

    /** 按放置状态临时构造方块实体；该方块没有方块实体时返回 null。 */
    private static @Nullable BlockEntity previewEntity(BlockState state) {
        if (!state.hasBlockEntity() || !(state.getBlock() instanceof EntityBlock entityBlock)) return null;
        BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
        if (entity == null) return null;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) entity.setLevel(level);
        return entity;
    }

    private static List<SelectionPart> parts(ClientLevel level, BlockPos pos, BlockState state, float partialTick) {
        ModelSelection model = snapshot.states().get(state);
        List<SelectionPart> parts = new ArrayList<>(ModelSelectionBakery.collect(model, state.getSeed(pos)));
        if (state.getBlock() instanceof EntityBlock) parts.addAll(dynamic(level, pos, partialTick));
        if (model == null || parts.isEmpty() && !snapshot.outlines().containsKey(state)) return fallback(level, pos, state);
        return parts;
    }

    static List<SelectionPart> dynamic(ClientLevel level, BlockPos pos, float partialTick) {
        return DYNAMIC.computeIfAbsent(pos.immutable(), key -> {
            BlockEntity entity = level.getBlockEntity(key);
            if (entity == null || ModelSelectionBlacklist.excludesBlockEntity(entity.getBlockState().getBlock())) return List.of();
            return rendererParts(entity, partialTick);
        });
    }

    /** 返回 BER 实际使用的 cube 和姿态，不混入用于交互的碰撞箱回退。 */
    public static <T extends BlockEntity> List<SelectionPart> rendererParts(T entity, float partialTick) {
        return ModelBlockSelection.rendererParts(entity, partialTick, false);
    }

    /**
     * @param preview 为 true 时走 {@link ModelSelectionRenderer#collectPreviewModels}，
     *                让依赖服务端同步状态的渲染器能给出放置预览用的静止姿态
     */
    private static <T extends BlockEntity> List<SelectionPart> rendererParts(
        T entity,
        float partialTick,
        boolean preview
    ) {
        BlockEntityRenderer<T> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof ModelSelectionRenderer<?>)) return List.of();
        @SuppressWarnings("unchecked")
        ModelSelectionRenderer<T> selectionRenderer = (ModelSelectionRenderer<T>) renderer;
        List<SelectionPart> result = new ArrayList<>();
        ModelSelectionRenderer.ModelConsumer consumer = (model, pose) -> {
            SelectionPart source = snapshot.standalone().get(model);
            if (source == null) return;
            pose.pushPose();
            source.apply(pose);
            Matrix4f transform = pose.last().pose();
            if (transform.isFinite() && transform.isAffine() && Math.abs(transform.determinant()) >= 1.0E-9) {
                result.add(new SelectionPart(source.geometry(), transform));
            }
            pose.popPose();
        };
        if (preview) {
            selectionRenderer.collectPreviewModels(entity, partialTick, new PoseStack(), consumer);
        } else {
            selectionRenderer.collectSelectionModels(entity, partialTick, new PoseStack(), consumer);
        }
        selectionRenderer.collectSelectionParts(entity, partialTick, result);
        return List.copyOf(result);
    }

    private static List<SelectionPart> fallback(ClientLevel level, BlockPos pos, BlockState state) {
        Minecraft minecraft = Minecraft.getInstance();
        CollisionContext context = minecraft.player == null ? CollisionContext.empty() : CollisionContext.of(minecraft.player);
        VoxelShape shape = state.getShape(level, pos, context);
        if (shape.isEmpty()) return List.of();
        SelectionPart part = FALLBACK.getIfPresent(shape);
        if (part == null) {
            List<AABB> boxes = shape.toAabbs();
            if (boxes.size() > SelectionGeometry.MAX_SHAPES) boxes = List.of(shape.bounds());
            List<ConvexShape> shapes = boxes.stream().map(box -> ConvexShape.box(new AABB(
                box.minX * ModelCubeGeometry.SCALE, box.minY * ModelCubeGeometry.SCALE, box.minZ * ModelCubeGeometry.SCALE,
                box.maxX * ModelCubeGeometry.SCALE, box.maxY * ModelCubeGeometry.SCALE, box.maxZ * ModelCubeGeometry.SCALE
            ))).toList();
            part = new SelectionPart(new SelectionGeometry(shapes), new Matrix4f().scaling(1 / ModelCubeGeometry.SCALE));
            FALLBACK.put(shape, part);
        }
        return List.of(part);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void highlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.options.hideGui) return;
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!AnvilCraft.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) return;
        if (ModelSelectionBlacklist.usesOriginalOutline(block)) return;
        boolean originalPicking = ModelSelectionBlacklist.usesOriginalPicking(block);
        if (!originalPicking && !CubeSelection.isEnabled(block)) return;
        List<SelectionPart> whole = snapshot.outlines().get(state);
        boolean multipartOutline = whole != null && block instanceof AbstractMultiPartBlock<?>;
        float tick = event.getDeltaTracker().getGameTimeDeltaPartialTick(
            !level.tickRateManager().isEntityFrozen(event.getCamera().getEntity())
        );
        List<SelectionPart> outline = multipartOutline ? whole : parts(level, pos, state, tick);
        if (outline.isEmpty() && !multipartOutline) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 offset = multipartOutline ? Vec3.ZERO : state.getOffset(level, pos);
        pose.pushPose();
        pose.translate(pos.getX() - camera.x + offset.x, pos.getY() - camera.y + offset.y, pos.getZ() - camera.z + offset.z);
        drawOutline(outline, pose, event);
        if (multipartOutline && block instanceof AbstractMultiPartBlock<?> multipart) {
            drawDynamicParts(multipart, state, pos, level, tick, pose, event);
        }
        pose.popPose();
        event.setCanceled(true);
    }

    private static <P extends Enum<P>> void drawDynamicParts(
        AbstractMultiPartBlock<P> block, BlockState state, BlockPos pos, ClientLevel level, float tick,
        PoseStack pose, RenderHighlightEvent.Block event
    ) {
        for (P part : block.getParts()) {
            BlockPos partPos = pos.offset(block.offsetFrom(state, part));
            if (!level.getBlockState(partPos).is(block)) continue;
            pose.pushPose();
            pose.translate(partPos.getX() - pos.getX(), partPos.getY() - pos.getY(), partPos.getZ() - pos.getZ());
            for (SelectionPart dynamic : dynamic(level, partPos, tick)) draw(dynamic, pose, event);
            pose.popPose();
        }
    }

    private static void draw(SelectionPart part, PoseStack pose, RenderHighlightEvent.Block event) {
        pose.pushPose();
        part.apply(pose);
        OutlineRenderer.render(pose, event.getMultiBufferSource().getBuffer(RenderType.lines()),
            CubeSelection.outlines().get(part.geometry()), 0, 0, 0, 0.4F);
        pose.popPose();
    }

    private static void drawOutline(List<SelectionPart> parts, PoseStack pose, RenderHighlightEvent.Block event) {
        int segments = 0;
        AABB bounds = null;
        for (SelectionPart part : parts) {
            segments += CubeSelection.outlines().get(part.geometry()).segmentCount();
            bounds = bounds == null ? part.bounds() : bounds.minmax(part.bounds());
        }
        if (segments > SelectionGeometry.MAX_OUTLINE_SEGMENTS && bounds != null) {
            LevelRenderer.renderLineBox(pose, event.getMultiBufferSource().getBuffer(RenderType.lines()), bounds, 0, 0, 0, 0.4F);
            return;
        }
        for (SelectionPart part : parts) draw(part, pose, event);
    }

    record Snapshot(Map<BlockState, ModelSelection> states, Map<BlockState, List<SelectionPart>> outlines,
                    Map<ModelResourceLocation, SelectionPart> standalone) {
    }
}
