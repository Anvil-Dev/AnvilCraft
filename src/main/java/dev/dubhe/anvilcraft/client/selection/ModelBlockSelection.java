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
import dev.dubhe.anvilcraft.block.FishTankBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.ProcessingTableBlock;
import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.mixin.accessor.ModelBakeryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ModelBlockSelection {
    private static long frame;
    private static Snapshot snapshot = new Snapshot(Map.of(), Map.of(), Map.of());
    private static final Map<BlockPos, List<SelectionPart>> DYNAMIC = new HashMap<>();
    private static final Cache<VoxelShape, SelectionPart> FALLBACK = CacheBuilder.newBuilder().weakKeys()
        .maximumWeight(4 * 1024 * 1024)
        .weigher((VoxelShape key, SelectionPart value) -> (int) value.geometry().estimatedBytes() + 512)
        .build();

    private ModelBlockSelection() {
    }

    public static void reload(ModelEvent.BakingCompleted event) {
        ModelBakeryAccessor bakery = (ModelBakeryAccessor) event.getModelBakery();
        snapshot = new ModelSelectionBakery(bakery.getUnbakedCache()).bake(bakery.getTopLevelModels());
        DYNAMIC.clear();
        FALLBACK.invalidateAll();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!AnvilCraft.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            if (usesOriginalPicking(block)) {
                CubeSelection.exclude(block);
                continue;
            }
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

    private static List<SelectionPart> parts(ClientLevel level, BlockPos pos, BlockState state, float partialTick) {
        ModelSelection model = snapshot.states().get(state);
        List<SelectionPart> parts = new ArrayList<>(ModelSelectionBakery.collect(model, state.getSeed(pos)));
        if (state.getBlock() instanceof EntityBlock) parts.addAll(dynamic(level, pos, partialTick));
        if (model == null || parts.isEmpty() && !snapshot.outlines().containsKey(state)) return fallback(level, pos, state);
        return parts;
    }

    private static boolean usesOriginalPicking(Block block) {
        return block instanceof FishTankBlock
            || block instanceof ProcessingTableBlock
            || block instanceof LargeCauldronBlock
            || block instanceof TradingStationBlock
            || block instanceof CrateBlock
            || block == ModBlocks.HEAVY_IRON_COLUMN.get();
    }

    static List<SelectionPart> dynamic(ClientLevel level, BlockPos pos, float partialTick) {
        return DYNAMIC.computeIfAbsent(pos.immutable(), key -> {
            BlockEntity entity = level.getBlockEntity(key);
            if (entity == null) return List.of();
            return collectDynamic(entity, partialTick);
        });
    }

    private static <T extends BlockEntity> List<SelectionPart> collectDynamic(T entity, float partialTick) {
        BlockEntityRenderer<T> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof ModelSelectionRenderer<?>)) return List.of();
        @SuppressWarnings("unchecked")
        ModelSelectionRenderer<T> selectionRenderer = (ModelSelectionRenderer<T>) renderer;
        List<SelectionPart> result = new ArrayList<>();
        selectionRenderer.collectSelectionModels(entity, partialTick, new PoseStack(), (model, pose) -> {
            SelectionPart source = snapshot.standalone().get(model);
            if (source == null) return;
            pose.pushPose();
            source.apply(pose);
            Matrix4f transform = pose.last().pose();
            if (transform.isFinite() && transform.isAffine() && Math.abs(transform.determinant()) >= 1.0E-9) {
                result.add(new SelectionPart(source.geometry(), transform));
            }
            pose.popPose();
        });
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
        if (block instanceof LargeCauldronBlock) return;
        boolean originalPicking = usesOriginalPicking(block);
        if (!originalPicking && !CubeSelection.isEnabled(block)) return;
        List<SelectionPart> whole = snapshot.outlines().get(state);
        boolean multipartOutline = whole != null && block instanceof AbstractMultiPartBlock<?>;
        if (!multipartOutline && !originalPicking) return;
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
        for (SelectionPart part : outline) draw(part, pose, event);
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

    record Snapshot(Map<BlockState, ModelSelection> states, Map<BlockState, List<SelectionPart>> outlines,
                    Map<ModelResourceLocation, SelectionPart> standalone) {
    }
}
