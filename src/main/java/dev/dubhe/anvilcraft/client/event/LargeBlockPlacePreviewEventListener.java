package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.item.PlaceInWaterBlockItem;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import dev.dubhe.anvilcraft.util.SegmentedActuator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(Dist.CLIENT)
public class LargeBlockPlacePreviewEventListener {
    private static int failBoundCooldown = 0;
    private static int failBoundErrorCooldown = 0;

    private static ItemStack currentItem = ItemStack.EMPTY;
    private static BlockPos currentPos = null;

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
        ItemStack item = inventory.getItem(inventory.selected);
        if (!(item.getItem() instanceof BlockItem)) {
            hand = InteractionHand.OFF_HAND;
            item = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!(item.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        // 多方块方块自成一体；标签内的单方块（红石类 / 物流类）走单方块预览
        boolean multiPart = blockItem.getBlock() instanceof AbstractMultiPartBlock<?>;
        if (!multiPart && !blockItem.getBlock().defaultBlockState().is(ModBlockTags.PLACEMENT_PREVIEW)) {
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
                useContext = BlockPlacementPicking.forPlacement(new UseOnContext(player, hand, target));
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
     * 描边（{@link #renderGhost}），故这里只负责算出落点与放置状态。</p>
     */
    private static void updateSingleBlockPreview(Minecraft mc, BlockItem blockItem, UseOnContext useContext) {
        Block block = blockItem.getBlock();
        BlockPlaceContext context = new BlockPlaceContext(useContext);
        BlockPos pos = context.getClickedPos();
        // 放不下（如压力板缺少支撑）时不显示鬼影，避免给出错误预期
        BlockState state = block.getStateForPlacement(context);
        if (mc.level != null && (state == null || !mc.level.getBlockState(pos).canBeReplaced(context))) {
            return;
        }
        if (state != null) {
            renderEntries.add(new RenderEntry(pos, state));
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
        return block instanceof AbstractMultiPartBlock<?>
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

    @SubscribeEvent
    public static void renderGhost(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.level == null) {
            renderEntries.clear();
            missingAmplifierAnvilPositions.clear();
            return;
        }
        renderMissingAmplifierGhosts(event);
        if (AnvilCraftClient.CONFIG.multiPartPreviewMode == AnvilCraftClientConfig.MultiPartPreviewMode.OFF) {
            renderEntries.clear();
            return;
        }
        updatePreview();
        if (renderEntries.isEmpty()) {
            return;
        }
        ItemStack item = player.getInventory().getItem(player.getInventory().selected);
        if (!(item.getItem() instanceof BlockItem)) {
            item = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!(item.getItem() instanceof BlockItem blockItem) || !isPreviewable(blockItem.getBlock())) {
            renderEntries.clear();
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        if (AnvilCraftClient.CONFIG.multiPartPreviewMode == AnvilCraftClientConfig.MultiPartPreviewMode.OUTLINE) {
            if (renderOutline(poseStack, bufferSource, event.getCamera())) {
                return;
            }
            expandRenderEntriesForGhost();
        }
        RenderType renderType = ModRenderTypes.BEACON_GLASS;
        float alpha = (float) AnvilCraftClient.CONFIG.multiPartPreviewGhostOpacity;
        int color = boundColor;
        float red = FastColor.ARGB32.red(color) / 255f;
        float green = FastColor.ARGB32.green(color) / 255f;
        float blue = FastColor.ARGB32.blue(color) / 255f;
        for (RenderEntry entry : renderEntries) {
            poseStack.pushPose();
            poseStack.translate(
                entry.pos().getX() - camera.x - 0.0005,
                entry.pos().getY() - camera.y - 0.0005,
                entry.pos().getZ() - camera.z - 0.0005
            );
            poseStack.scale(1.001f, 1.001f, 1.001f);
            renderPart(poseStack, bufferSource, renderType, entry.state(), alpha, red, green, blue);
            poseStack.popPose();
        }
        // 方块实体模型（如智能方块放置器的机械臂）不属于方块模型，按各自位姿单独渲染
        RenderEntry base = renderEntries.getFirst();
        for (ModelBlockSelection.ModelPlacement placement : ModelBlockSelection.previewBerModels(base.state(), base.pos())) {
            poseStack.pushPose();
            poseStack.translate(
                base.pos().getX() - camera.x - 0.0005,
                base.pos().getY() - camera.y - 0.0005,
                base.pos().getZ() - camera.z - 0.0005
            );
            poseStack.scale(1.001f, 1.001f, 1.001f);
            poseStack.last().pose().mul(placement.pose());
            // 与 BER 一致地传 null 状态，避免对独立模型套用方块着色
            renderModel(
                poseStack,
                bufferSource,
                renderType,
                mc.getModelManager().getModel(placement.model()),
                null,
                alpha,
                red,
                green,
                blue
            );
            poseStack.popPose();
        }
        renderErrorBound(poseStack, bufferSource, event.getCamera());
        bufferSource.endBatch(renderType);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderMissingAmplifierGhosts(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            missingAmplifierAnvilPositions.clear();
            return;
        }
        long now = Util.getMillis();
        missingAmplifierAnvilPositions.entrySet().removeIf(entry -> now >= entry.getValue()
            || !(level.getBlockEntity(entry.getKey()) instanceof CelestialForgingAnvilBlockEntity anvil)
            || anvil.isRemoved() || anvil.isAmplifierPresent());
        if (missingAmplifierAnvilPositions.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        CelestialForgingAnvilAmplifierBlock amplifier = ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get();
        boolean outlineMode = AnvilCraftClient.CONFIG.multiPartPreviewMode
            != AnvilCraftClientConfig.MultiPartPreviewMode.GHOST;
        RenderType renderType = outlineMode ? RenderType.lines() : ModRenderTypes.BEACON_GLASS;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        if (outlineMode) {
            if (level != null) {
                renderMissingAmplifierOutlines(poseStack, vertexConsumer, cameraPos, amplifier, level);
            }
        } else {
            if (level != null) {
                renderMissingAmplifierGlass(poseStack, bufferSource, renderType, cameraPos, amplifier, level);
            }
        }
        bufferSource.endBatch(renderType);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderMissingAmplifierOutlines(
        PoseStack poseStack,
        VertexConsumer vertexConsumer,
        Vec3 cameraPos,
        CelestialForgingAnvilAmplifierBlock amplifier,
        Level level
    ) {
        for (BlockPos anvilPos : missingAmplifierAnvilPositions.keySet()) {
            for (int i = 0; i < AMPLIFIER_CORNER_OFFSETS.length; i++) {
                BlockPos mainPos = anvilPos.offset(AMPLIFIER_CORNER_OFFSETS[i]);
                if (level.getBlockState(mainPos).is(amplifier)) {
                    continue;
                }
                BlockState state = amplifier.defaultBlockState()
                    .setValue(CelestialForgingAnvilAmplifierBlock.FACING, AMPLIFIER_CORNER_FACINGS[i]);
                List<SelectionPart> outline = ModelBlockSelection.multipartOutline(state);
                if (outline.isEmpty()) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(
                    mainPos.getX() - cameraPos.x,
                    mainPos.getY() - cameraPos.y,
                    mainPos.getZ() - cameraPos.z
                );
                for (SelectionPart selectionPart : outline) {
                    poseStack.pushPose();
                    selectionPart.apply(poseStack);
                    OutlineRenderer.render(poseStack, vertexConsumer,
                        CubeSelection.outlines().get(selectionPart.geometry()), 1.0f, 1.0f, 1.0f,
                        (float) AnvilCraftClient.CONFIG.multiPartPreviewOutlineOpacity);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
        }
    }

    private static void renderMissingAmplifierGlass(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        RenderType renderType,
        Vec3 cameraPos,
        CelestialForgingAnvilAmplifierBlock amplifier,
        Level level
    ) {
        for (BlockPos anvilPos : missingAmplifierAnvilPositions.keySet()) {
            for (int i = 0; i < AMPLIFIER_CORNER_OFFSETS.length; i++) {
                BlockPos mainPos = anvilPos.offset(AMPLIFIER_CORNER_OFFSETS[i]);
                if (level.getBlockState(mainPos).is(amplifier)) {
                    continue;
                }
                BlockState state = amplifier.defaultBlockState()
                    .setValue(CelestialForgingAnvilAmplifierBlock.FACING, AMPLIFIER_CORNER_FACINGS[i]);
                for (DirectionCube232PartHalf part : amplifier.getParts()) {
                    BlockPos pos = mainPos.offset(amplifier.offsetFrom(state, part));
                    poseStack.pushPose();
                    poseStack.translate(
                        pos.getX() - cameraPos.x,
                        pos.getY() - cameraPos.y,
                        pos.getZ() - cameraPos.z
                    );
                    poseStack.scale(1.001f, 1.001f, 1.001f);
                    BlockState partState = amplifier.placedState(part, state);
                    renderPart(poseStack, bufferSource, renderType, partState,
                        (float) AnvilCraftClient.CONFIG.multiPartPreviewGhostOpacity, 1.0f, 1.0f, 1.0f);
                    poseStack.popPose();
                }
            }
        }
    }

    private static boolean renderOutline(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Camera camera
    ) {
        if (renderEntries.isEmpty()) {
            return false;
        }
        RenderEntry base = renderEntries.getFirst();
        List<SelectionPart> outline = new ArrayList<>(ModelBlockSelection.multipartOutline(base.state()));
        // 方块实体模型是独立模型，不在方块模型描边表里，需单独并入（如智能方块放置器的机械臂）
        // 传入真实放置位：渲染器读自身坐标处的世界状态（如比较器读 POWER）时才有正确姿态
        outline.addAll(ModelBlockSelection.previewBerParts(base.state(), base.pos()));
        if (outline.isEmpty()) {
            return false;
        }
        Vec3 cameraPos = camera.getPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(
            base.pos().getX() - cameraPos.x,
            base.pos().getY() - cameraPos.y,
            base.pos().getZ() - cameraPos.z
        );
        int color = boundColor;
        float red = FastColor.ARGB32.red(color) / 255f;
        float green = FastColor.ARGB32.green(color) / 255f;
        float blue = FastColor.ARGB32.blue(color) / 255f;
        for (SelectionPart part : outline) {
            poseStack.pushPose();
            part.apply(poseStack);
            OutlineRenderer.render(poseStack, vertexConsumer,
                CubeSelection.outlines().get(part.geometry()), red, green, blue,
                (float) AnvilCraftClient.CONFIG.multiPartPreviewOutlineOpacity);
            poseStack.popPose();
        }
        poseStack.popPose();
        renderErrorBound(poseStack, bufferSource, camera);
        bufferSource.endBatch(RenderType.lines());
        return true;
    }

    private static void renderErrorBound(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Camera camera
    ) {
        if (failBoundErrorCooldown <= 0) {
            return;
        }
        Vec3 position = camera.getPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        for (BlockPos blockPos : cachedErrorPosList) {
            TooltipRenderHelper.renderOutline(
                poseStack,
                vertexConsumer,
                position.x,
                position.y,
                position.z,
                blockPos,
                Shapes.block(),
                0xffff0000
            );
        }
    }

    private static void renderPart(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        RenderType renderType,
        BlockState state,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        renderModel(
            poseStack,
            bufferSource,
            renderType,
            Minecraft.getInstance().getBlockRenderer().getBlockModel(state),
            state,
            alpha,
            red,
            green,
            blue
        );
    }

    private static void renderModel(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        RenderType renderType,
        BakedModel model,
        @Nullable BlockState state,
        float alpha,
        float red,
        float green,
        float blue
    ) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        dispatcher.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            state,
            model,
            red,
            green,
            blue,
            LightTexture.FULL_BLOCK,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            renderType
        );
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
