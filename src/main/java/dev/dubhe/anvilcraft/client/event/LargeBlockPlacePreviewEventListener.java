package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.cake.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.ChuteBlockItem;
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.block.LargeCakeBlockItem;
import dev.dubhe.anvilcraft.item.block.PlaceInWaterBlockItem;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import dev.dubhe.anvilcraft.util.SegmentedActuator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(Dist.CLIENT)
public class LargeBlockPlacePreviewEventListener {
    private static int failBoundCooldown = 0;
    private static int failBoundErrorCooldown = 0;

    private static ItemStack currentItem = ItemStack.EMPTY;
    private static @Nullable BlockPos currentPos;

    private static int boundColor = 0xffffffff;
    private static List<BlockPos> cachedErrorPosList = new ObjectArrayList<>();

    private static final Runnable changeBoundColorRed = () -> boundColor = 0xffff0000;
    private static final Runnable changeBoundColorWhite = () -> boundColor = 0xffffffff;

    private static final SegmentedActuator animationActuator = new SegmentedActuator(
        new SegmentedActuator.Task(20, changeBoundColorRed),
        new SegmentedActuator.Task(20, changeBoundColorWhite),
        new SegmentedActuator.Task(20, changeBoundColorRed),
        new SegmentedActuator.Task(20, changeBoundColorWhite)
    );

    private static final ObjectArrayList<RenderEntry> renderEntries = new ObjectArrayList<>();

    private static final long MISSING_AMPLIFIER_PREVIEW_DURATION_MS = 10_000L;
    private static final Map<BlockPos, Long> missingAmplifierAnvilPositions = new HashMap<>();
    private static final BlockPos[] AMPLIFIER_CORNER_OFFSETS = {
        new BlockPos(-2, 0, -2),
        new BlockPos(3, 0, -2),
        new BlockPos(-2, 0, 3),
        new BlockPos(3, 0, 3),
    };
    private static final Direction[] AMPLIFIER_CORNER_FACINGS = {
        Direction.NORTH,
        Direction.EAST,
        Direction.WEST,
        Direction.SOUTH,
    };

    private record RenderEntry(BlockPos pos, BlockState state) {
    }

    public static void offerMissingAmplifierAnvil(BlockPos anvilPos) {
        missingAmplifierAnvilPositions.put(anvilPos.immutable(), Util.getMillis() + MISSING_AMPLIFIER_PREVIEW_DURATION_MS);
    }

    public static void removeMissingAmplifierAnvil(BlockPos anvilPos) {
        missingAmplifierAnvilPositions.remove(anvilPos);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            missingAmplifierAnvilPositions.clear();
            renderEntries.clear();
        }
    }

    private static void updatePreview() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.level == null) {
            return;
        }
        boundColor = 0xffffffff;
        if (failBoundCooldown > 0) {
            failBoundCooldown--;
            animationActuator.execute();
        }
        if (failBoundErrorCooldown > 0) {
            failBoundErrorCooldown--;
        }
        renderEntries.clear();
        Inventory inventory = player.getInventory();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack item = inventory.getSelectedItem();
        if (!(item.getItem() instanceof BlockItem)) {
            hand = InteractionHand.OFF_HAND;
            item = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!(item.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        // 多方块方块自成一体；标签内的单方块（红石类 / 物流类）走单方块预览
        final boolean multiPart = blockItem.getBlock() instanceof AbstractMultiPartBlock<?>;
        if (!isPreviewable(blockItem.getBlock())) {
            return;
        }
        UseOnContext useContext;
        if (item.getItem() instanceof PlaceInWaterBlockItem) {
            // 这类物品只在水面放置：useOn() 返回 PASS，实际落点由 use() 用流体射线
            // （Fluid.SOURCE_ONLY）取得。而准星拾取用的是 Fluid.NONE，且水方块 getShape()
            // 为空，水面根本不会出现在 mc.hitResult 里（还可能被前方实体挡成 EntityHitResult），
            // 故这里不依赖 mc.hitResult，按放置逻辑同样的流体射线重算落点。
            BlockHitResult fluidHit = Item.getPlayerPOVHitResult(mc.level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (fluidHit.getType() == HitResult.Type.MISS) {
                return;
            }
            useContext = new UseOnContext(mc.level, player, hand, item, fluidHit.withPosition(fluidHit.getBlockPos()));
        } else {
            if (!(mc.hitResult instanceof BlockHitResult target)) {
                return;
            }
            if (target.getType() == HitResult.Type.MISS) {
                BlockHitResult hit = BlockPlacementPicking.findAirPlacementHit(item, mc.level, player);
                if (hit == null) return;
                useContext = new UseOnContext(mc.level, player, hand, item, hit);
            } else {
                useContext = new UseOnContext(player, hand, target);
                if (blockItem instanceof ChuteBlockItem && ChuteBlockItem.isStorageInteraction(useContext)) {
                    return;
                }
                useContext = BlockPlacementPicking.forPlacement(useContext);
            }
        }
        if (useContext instanceof BlockPlacementPicking.PlayerClick click && !click.anvilcraft$hasBlockHit()) {
            return;
        }
        if (!multiPart) {
            updateSingleBlockPreview(mc, blockItem, useContext);
            return;
        }
        final Direction direction = useContext.getClickedFace();
        AbstractMultiPartBlock<?> block = (AbstractMultiPartBlock<?>) blockItem.getBlock();
        BlockPlaceContext context = snapPlacementContext(block, new BlockPlaceContext(useContext));
        BlockPos pos = context.getClickedPos();
        validateCanRender(item, blockItem, pos);
        BlockState state = getPlacementState(block, blockItem, context);
        List<BlockPos> errorPosList = getErrorPosList(mc.level, block, pos, state);
        if (!errorPosList.isEmpty()) {
            if (blockItem instanceof SimpleMultiPartBlockItem<?> simpleMultiPartBlockItem) {
                int distance = simpleMultiPartBlockItem.getMaxOffsetDistance(direction);
                pos = useContext.getClickedPos().relative(direction, distance);
            }
            if (blockItem instanceof FlexibleMultiPartBlockItem<?, ?, ?> flexibleMultiPartBlockItem) {
                int distance = flexibleMultiPartBlockItem.getMaxOffsetDistance(state, direction);
                pos = useContext.getClickedPos().relative(direction, distance);
            }
            context = snapPlacementContext(block, new BlockPlaceContext(new UseOnContext(
                mc.level, player, hand, item, new BlockHitResult(
                    useContext.getClickLocation().add(Vec3.atLowerCornerOf(pos.subtract(useContext.getClickedPos()))),
                    direction,
                    pos,
                    false
                )
            )));
            pos = context.getClickedPos();
            state = getPlacementState(block, blockItem, context);
            errorPosList = getErrorPosList(mc.level, block, pos, state);
        }
        if (errorPosList.isEmpty()) {
            collectRenderEntries(block, pos, state);
        }
    }

    /**
     * 单方块放置预览：按放置状态在落点渲染一个鬼影。
     *
     * <p>红石类与物流类的朝向 / 贴面由点击位置决定（溜槽会自动背对玩家、红石导线贴在
     * 被点击的面、滑轨沿视线轴向），先看一眼朝向能避免放错。渲染复用多方块的鬼影与
     * 描边（{@link #extractPreview}），故这里只负责算出落点与放置状态。</p>
     */
    private static void updateSingleBlockPreview(Minecraft mc, BlockItem blockItem, UseOnContext useContext) {
        Block block = blockItem.getBlock();
        BlockPlaceContext context = new BlockPlaceContext(useContext);
        BlockPos pos = context.getClickedPos();
        // 放不下（如压力板缺少支撑）时不显示鬼影，避免给出错误预期
        BlockState state = blockItem instanceof LargeCakeBlockItem cake ? cake.getPlacementState(context)
            : block.getStateForPlacement(context);
        if (mc.level != null && (state == null || !mc.level.getBlockState(pos).canBeReplaced(context))) {
            return;
        }
        if (state != null) {
            if (blockItem instanceof LargeCakeBlockItem) {
                LargeCakeBlockItem.forEachPlacedBlock(pos, state,
                    (partPos, partState) -> renderEntries.add(new RenderEntry(partPos, partState)));
            } else {
                renderEntries.add(new RenderEntry(pos, state));
            }
        }
    }

    private static BlockPlaceContext snapPlacementContext(AbstractMultiPartBlock<?> block, BlockPlaceContext context) {
        if (!(block instanceof CelestialForgingAnvilAmplifierBlock amplifier)) return context;
        BlockPos pos = context.getClickedPos();
        BlockPos snapped = amplifier.snapMainPos(context.getLevel(), pos);
        if (snapped == null || snapped.equals(pos)) return context;
        return new BlockPlaceContext(
            context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), new BlockHitResult(
                context.getClickLocation().add(Vec3.atLowerCornerOf(snapped.subtract(pos))),
                context.getClickedFace(),
                snapped,
                false
            )
        );
    }

    /** 该方块是否参与放置预览：多方块方块，或 {@link ModBlockTags#PLACEMENT_PREVIEW} 内的单方块。 */
    private static boolean isPreviewable(Block block) {
        return block instanceof AbstractMultiPartBlock<?> || block instanceof LargeCakeBlock
               || block.defaultBlockState().is(ModBlockTags.PLACEMENT_PREVIEW);
    }

    private static void expandRenderEntriesForGhost() {
        RenderEntry base = renderEntries.getFirst();
        if (!(base.state().getBlock() instanceof AbstractMultiPartBlock<?> block)) {
            return;
        }
        ObjectArrayList<RenderEntry> parts = new ObjectArrayList<>();
        for (Enum<?> part : block.getParts()) {
            BlockPos partPos = base.pos().offset(block.offsetFrom(base.state(), cast(part)));
            parts.add(new RenderEntry(partPos, block.placedState(cast(part), base.state())));
        }
        renderEntries.clear();
        renderEntries.addAll(parts);
    }

    private static List<BlockPos> getErrorPosList(
        Level level,
        AbstractMultiPartBlock<?> block,
        BlockPos pos,
        BlockState state
    ) {
        List<BlockPos> errorBlockPosList = new ObjectArrayList<>();
        for (Enum<?> part : block.getParts()) {
            BlockPos offset = pos.offset(block.offsetFrom(state, cast(part)));
            BlockState blockState = level.getBlockState(offset);
            if (!blockState.canBeReplaced() || level.isOutsideBuildHeight(offset)) {
                errorBlockPosList.add(offset);
            }
        }
        return errorBlockPosList;
    }

    private static void collectRenderEntries(AbstractMultiPartBlock<?> block, BlockPos pos, BlockState state) {
        if (AnvilCraftClient.CONFIG.multiPartPreviewMode == AnvilCraftClientConfig.MultiPartPreviewMode.OUTLINE) {
            renderEntries.add(new RenderEntry(pos, state));
            return;
        }
        for (Enum<?> part : block.getParts()) {
            BlockPos partPos = pos.offset(block.offsetFrom(state, cast(part)));
            BlockState partState = block.placedState(cast(part), state);
            renderEntries.add(new RenderEntry(partPos, partState));
        }
    }

    private static final ContextKey<PreviewFrame> PREVIEW = new ContextKey<>(AnvilCraft.of("placement_preview"));

    private record OutlineBatch(BlockPos pos, List<SelectionPart> parts, int color, float alpha) {
    }

    private record GhostBatch(BlockPos pos, Matrix4f pose, List<BakedQuad> quads, int color, double inset) {
    }

    private record PreviewFrame(List<OutlineBatch> outlines, List<GhostBatch> ghosts, List<BlockPos> errors) {
    }

    @SubscribeEvent
    public static void extractPreview(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.isSpectator()) {
            renderEntries.clear();
            missingAmplifierAnvilPositions.clear();
            return;
        }
        List<OutlineBatch> outlines = new ArrayList<>();
        List<GhostBatch> ghosts = new ArrayList<>();
        collectMissingAmplifiers(mc.level, outlines, ghosts);
        if (AnvilCraftClient.CONFIG.multiPartPreviewMode != AnvilCraftClientConfig.MultiPartPreviewMode.OFF) {
            updatePreview();
            if (!renderEntries.isEmpty()) {
                RenderEntry base = renderEntries.getFirst();
                boolean outlined = false;
                if (AnvilCraftClient.CONFIG.multiPartPreviewMode == AnvilCraftClientConfig.MultiPartPreviewMode.OUTLINE) {
                    for (RenderEntry entry : renderEntries) {
                        List<SelectionPart> parts = new ArrayList<>(ModelBlockSelection.multipartOutline(entry.state()));
                        parts.addAll(ModelBlockSelection.previewBerParts(entry.state(), entry.pos()));
                        if (!parts.isEmpty()) {
                            outlines.add(new OutlineBatch(entry.pos(), List.copyOf(parts), boundColor,
                                (float) AnvilCraftClient.CONFIG.multiPartPreviewOutlineOpacity));
                            outlined = true;
                        }
                    }
                    if (!outlined) expandRenderEntriesForGhost();
                }
                if (!outlined) {
                    int color = ARGB.color((int) (255 * AnvilCraftClient.CONFIG.multiPartPreviewGhostOpacity), boundColor);
                    for (RenderEntry entry : renderEntries) {
                        collectGhost(entry, new Matrix4f(), null, color, ghosts, -0.0005);
                    }
                    for (ModelBlockSelection.ModelPlacement placement : ModelBlockSelection.previewBerModels(base.state(), base.pos())) {
                        collectGhost(base, placement.pose(), placement.model(), color, ghosts, -0.0005);
                    }
                }
            }
        } else {
            renderEntries.clear();
        }
        List<BlockPos> errors = !renderEntries.isEmpty() && failBoundErrorCooldown > 0 ? List.copyOf(cachedErrorPosList) : List.of();
        event.getRenderState().setRenderData(PREVIEW, new PreviewFrame(List.copyOf(outlines), List.copyOf(ghosts), errors));
    }

    private static void collectMissingAmplifiers(Level level, List<OutlineBatch> outlines, List<GhostBatch> ghosts) {
        long now = Util.getMillis();
        missingAmplifierAnvilPositions.entrySet().removeIf(entry -> now >= entry.getValue()
            || !(level.getBlockEntity(entry.getKey()) instanceof CelestialForgingAnvilBlockEntity anvil)
            || anvil.isRemoved() || anvil.isAmplifierPresent());
        CelestialForgingAnvilAmplifierBlock amplifier = ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get();
        boolean outline = AnvilCraftClient.CONFIG.multiPartPreviewMode != AnvilCraftClientConfig.MultiPartPreviewMode.GHOST;
        for (BlockPos anvilPos : missingAmplifierAnvilPositions.keySet()) {
            for (int i = 0; i < AMPLIFIER_CORNER_OFFSETS.length; i++) {
                BlockPos mainPos = anvilPos.offset(AMPLIFIER_CORNER_OFFSETS[i]);
                if (level.getBlockState(mainPos).is(amplifier)) continue;
                BlockState state = amplifier.defaultBlockState().setValue(CelestialForgingAnvilAmplifierBlock.FACING,
                    AMPLIFIER_CORNER_FACINGS[i]);
                if (outline) {
                    List<SelectionPart> parts = ModelBlockSelection.multipartOutline(state);
                    if (!parts.isEmpty()) {
                        outlines.add(new OutlineBatch(mainPos, List.copyOf(parts), -1,
                            (float) AnvilCraftClient.CONFIG.multiPartPreviewOutlineOpacity));
                    }
                } else {
                    int color = ARGB.color((int) (255 * AnvilCraftClient.CONFIG.multiPartPreviewGhostOpacity), -1);
                    for (var part : amplifier.getParts()) {
                        collectGhost(new RenderEntry(mainPos.offset(amplifier.offsetFrom(state, part)), amplifier.placedState(part, state)),
                            new Matrix4f(), null, color, ghosts, 0);
                    }
                }
            }
        }
    }

    private static void collectGhost(
        RenderEntry entry, Matrix4f pose, @Nullable SelectionModel selected, int color, List<GhostBatch> ghosts, double inset
    ) {
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel model;
        if (selected instanceof SelectionModel.Standalone standalone) {
            Object standaloneModel = mc.getModelManager().getStandaloneModel(standalone.key());
            if (!(standaloneModel instanceof BlockStateModel blockModel)) return;
            model = blockModel;
        } else {
            BlockState state = selected instanceof SelectionModel.State stateModel ? stateModel.state() : entry.state();
            model = mc.getModelManager().getBlockStateModelSet().get(state);
        }
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(mc.level, entry.pos(), entry.state(), RandomSource.create(42), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
            quads.addAll(part.getQuads(null));
        }
        if (!quads.isEmpty()) ghosts.add(new GhostBatch(entry.pos(), new Matrix4f(pose), List.copyOf(quads), color, inset));
    }

    @SubscribeEvent
    public static void submitPreview(SubmitCustomGeometryEvent event) {
        PreviewFrame frame = event.getLevelRenderState().getRenderData(PREVIEW);
        if (frame == null) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        for (GhostBatch batch : frame.ghosts()) {
            pose.pushPose();
            pose.translate(batch.pos().getX() - camera.x + batch.inset(), batch.pos().getY() - camera.y + batch.inset(),
                batch.pos().getZ() - camera.z + batch.inset());
            pose.scale(1.001F, 1.001F, 1.001F);
            pose.mulPose(batch.pose());
            event.getSubmitNodeCollector().submitCustomGeometry(pose, ModRenderTypes.PLACEMENT_GHOST, (renderPose, consumer) -> {
                QuadInstance instance = new QuadInstance();
                instance.setLightCoords(240);
                instance.setColor(batch.color());
                for (BakedQuad quad : batch.quads()) consumer.putBakedQuad(renderPose, quad, instance);
            });
            pose.popPose();
        }
        for (OutlineBatch batch : frame.outlines()) {
            pose.pushPose();
            pose.translate(batch.pos().getX() - camera.x, batch.pos().getY() - camera.y, batch.pos().getZ() - camera.z);
            for (SelectionPart part : batch.parts()) {
                pose.pushPose();
                part.apply(pose);
                var outline = CubeSelection.outlines().get(part.geometry());
                event.getSubmitNodeCollector().submitCustomGeometry(pose, RenderTypes.lines(), (renderPose, consumer) -> {
                    PoseStack stack = new PoseStack();
                    stack.last().set(renderPose);
                    OutlineRenderer.render(stack, consumer, outline, ARGB.red(batch.color()) / 255F,
                        ARGB.green(batch.color()) / 255F, ARGB.blue(batch.color()) / 255F, batch.alpha());
                });
                pose.popPose();
            }
            pose.popPose();
        }
        if (!frame.errors().isEmpty()) {
            event.getSubmitNodeCollector().submitCustomGeometry(pose, RenderTypes.lines(), (renderPose, consumer) -> {
                PoseStack stack = new PoseStack();
                stack.last().set(renderPose);
                for (BlockPos pos : frame.errors()) {
                    TooltipRenderHelper.renderOutline(stack, consumer, camera.x, camera.y, camera.z, pos, Shapes.block(), 0xffff0000);
                }
            });
        }
    }

    private static void validateCanRender(ItemStack item, BlockItem blockItem, BlockPos pos) {
        if (currentItem.isEmpty()) {
            currentItem = item.copy();
        } else if (!currentItem.is(blockItem)) {
            currentItem = ItemStack.EMPTY;
            failBoundCooldown = 0;
        }
        if (currentPos == null) {
            currentPos = pos;
        } else if (!currentPos.equals(pos)) {
            currentPos = null;
            failBoundCooldown = 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static <P extends Enum<P>> P cast(Enum<?> e) {
        return (P) e;
    }

    private static BlockState getPlacementState(
        AbstractMultiPartBlock<?> block,
        BlockItem blockItem,
        BlockPlaceContext context
    ) {
        if (blockItem instanceof FlexibleMultiPartBlockItem<?, ?, ?> flexibleMultiPartBlockItem) {
            FlexibleMultiPartBlock<?, ?, ?> flexBlock = flexibleMultiPartBlockItem.getBlock();
            BlockState state = flexBlock.getPlacementState(context);
            return state != null ? state : block.defaultBlockState();
        }
        BlockState state = block.getStateForPlacement(context);
        return state != null ? state : block.defaultBlockState();
    }

    public static void startFailBoundCooldown() {
        failBoundCooldown = 80;
        animationActuator.reset();
    }

    public static void startFailBoundErrorCooldown(List<BlockPos> errorPosList) {
        failBoundErrorCooldown = 60;
        cachedErrorPosList = new ObjectArrayList<>(errorPosList);
    }
}
