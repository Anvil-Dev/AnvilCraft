package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import dev.dubhe.anvilcraft.util.SegmentedActuator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;
import javax.annotation.Nullable;

@EventBusSubscriber(Dist.CLIENT)
public class LargeBlockPlacePreviewEventListener {
    private static int failBoundCooldown = 0;
    private static int failBoundErrorCooldown = 0;

    private static ItemStack currentItem = ItemStack.EMPTY;
    @Nullable
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

    private static final ObjectArrayList<BlockPos> missingAmplifierAnvilPositions = new ObjectArrayList<>();
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
        if (!missingAmplifierAnvilPositions.contains(anvilPos)) {
            missingAmplifierAnvilPositions.add(anvilPos);
        }
    }

    public static void removeMissingAmplifierAnvil(BlockPos anvilPos) {
        missingAmplifierAnvilPositions.remove(anvilPos);
    }

    @SubscribeEvent
    public static void renderHighlight(RenderHighlightEvent.Block event) {
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
        if (!(blockItem.getBlock() instanceof AbstractMultiPartBlock<?> block)) {
            return;
        }
        // 实际放置会把点击上下文交给 BlockPlacementPicking.forPlacement 重做一次射线检测，
        // 用的是常规形状（BlockGetter#clip / ClipContext.Block.OUTLINE → BlockState#getShape）；
        // 而准星射线走的是 CubeSelection 的模型精确箱（CubePicking#pick），两者命中格可能不同。
        // 预览必须走同一条路径，否则会与实际落点错位。
        // 注意 click 是 UseOnContext：它的 getClickedPos() 即原始命中格，重试分支用的就是它；
        // 而 BlockPlaceContext.getClickedPos() 在命中格不可替换时还会沿点击面外移一格。
        BlockHitResult target = event.getTarget();
        UseOnContext click = BlockPlacementPicking.forPlacement(new UseOnContext(player, hand, target));
        BlockPlaceContext context = new BlockPlaceContext(click);
        final BlockPos hitPos = click.getClickedPos();
        final Direction face = click.getClickedFace();
        if (block instanceof CelestialForgingAnvilAmplifierBlock amplifierBlock) {
            BlockPos snapped = amplifierBlock.snapMainPos(mc.level, context.getClickedPos());
            if (snapped != null) {
                context = new BlockPlaceContext(player, hand, item, new BlockHitResult(
                    target.getLocation(),
                    face,
                    snapped,
                    target.isInside()
                ));
            }
        }
        BlockPos pos = context.getClickedPos();
        validateCanRender(item, blockItem, pos);
        BlockState state = getPlacementState(block, blockItem, context);
        boolean placeable = isPlaceable(mc.level, player, block, pos, state);
        if (!placeable) {
            // 放不下时物品沿点击面退到偏移位置重试（SimpleMultiPartBlockItem#useOn），
            // 基准格与点击面都取原始命中的 click，与实际一致
            if (blockItem instanceof SimpleMultiPartBlockItem<?> simpleMultiPartBlockItem) {
                pos = hitPos.relative(face, simpleMultiPartBlockItem.getMaxOffsetDistance(face));
            } else if (blockItem instanceof FlexibleMultiPartBlockItem<?, ?, ?> flexibleMultiPartBlockItem) {
                pos = hitPos.relative(face, flexibleMultiPartBlockItem.getMaxOffsetDistance(state, face));
            }
            context = new BlockPlaceContext(player, hand, item, new BlockHitResult(
                Vec3.atCenterOf(pos),
                face,
                pos,
                false
            ));
            state = getPlacementState(block, blockItem, context);
            pos = context.getClickedPos();
            placeable = isPlaceable(mc.level, player, block, pos, state);
        }
        if (placeable) {
            collectRenderEntries(block, pos, state);
        }
    }

    /**
     * 复刻实际放置的合法性判断：各部件位置可替换，且状态可存活、目标格无实体阻挡
     * （{@code BlockItem#getPlacementState} → {@code BlockItem#canPlace}）。任一不满足时
     * 物品会判定放置失败并退回偏移位置，预览做同样判断才能与实际落点一致。
     */
    private static boolean isPlaceable(
        Level level,
        LocalPlayer player,
        AbstractMultiPartBlock<?> block,
        BlockPos pos,
        BlockState state
    ) {
        if (!getErrorPosList(level, block, pos, state).isEmpty()) return false;
        return state.canSurvive(level, pos) && level.isUnobstructed(state, pos, CollisionContext.of(player));
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
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            renderEntries.clear();
            return;
        }
        if (renderEntries.isEmpty()) {
            return;
        }
        ItemStack item = player.getInventory().getItem(player.getInventory().selected);
        if (!(item.getItem() instanceof BlockItem)) {
            item = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!(item.getItem() instanceof BlockItem blockItem)
            || !(blockItem.getBlock() instanceof AbstractMultiPartBlock<?>)) {
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
        renderErrorBound(poseStack, bufferSource, event.getCamera());
        bufferSource.endBatch(renderType);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderMissingAmplifierGhosts(RenderLevelStageEvent event) {
        if (missingAmplifierAnvilPositions.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        CelestialForgingAnvilAmplifierBlock amplifier = ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get();
        Level level = mc.level;
        boolean outlineMode = AnvilCraftClient.CONFIG.multiPartPreviewMode
            == AnvilCraftClientConfig.MultiPartPreviewMode.OUTLINE;
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
        for (BlockPos anvilPos : missingAmplifierAnvilPositions) {
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
        for (BlockPos anvilPos : missingAmplifierAnvilPositions) {
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
        List<SelectionPart> outline = ModelBlockSelection.multipartOutline(base.state());
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
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
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
