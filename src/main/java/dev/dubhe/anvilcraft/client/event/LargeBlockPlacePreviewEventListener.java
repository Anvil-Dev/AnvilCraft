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
import dev.dubhe.anvilcraft.block.item.PlaceInWaterBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import dev.dubhe.anvilcraft.building.BuildingRodService;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import dev.dubhe.anvilcraft.util.PlacementInteractions;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@EventBusSubscriber(Dist.CLIENT)
public class LargeBlockPlacePreviewEventListener {
    private static int failBoundCooldown = 0;
    private static int failBoundErrorCooldown = 0;

    private static ItemStack currentItem = ItemStack.EMPTY;
    @Nullable
    private static BlockPos currentPos;

    private static int boundColor = 0xffffffff;
    private static List<BlockPos> cachedErrorPosList = new ObjectArrayList<>();

    private static final Runnable changeBoundColorRed = () -> LargeBlockPlacePreviewEventListener.boundColor = 0xffff0000;
    private static final Runnable changeBoundColorWhite = () -> LargeBlockPlacePreviewEventListener.boundColor = 0xffffffff;

    private static final SegmentedActuator animationActuator = new SegmentedActuator(
        new SegmentedActuator.Task(20, LargeBlockPlacePreviewEventListener.changeBoundColorRed),
        new SegmentedActuator.Task(20, LargeBlockPlacePreviewEventListener.changeBoundColorWhite),
        new SegmentedActuator.Task(20, LargeBlockPlacePreviewEventListener.changeBoundColorRed),
        new SegmentedActuator.Task(20, LargeBlockPlacePreviewEventListener.changeBoundColorWhite)
    );

    private static final ObjectArrayList<RenderEntry> renderEntries = new ObjectArrayList<>();

    private static final long MISSING_AMPLIFIER_PREVIEW_DURATION_MS = 10_000L;
    private static final Map<BlockPos, Long> missingAmplifierAnvilPositions = new HashMap<>();
    private static final BlockPos[] AMPLIFIER_CORNER_OFFSETS = {
        new BlockPos(-2, 0, -2),
        new BlockPos(3, 0, -2),
        new BlockPos(-2, 0, 3),
        new BlockPos(3, 0, 3)
    };
    private static final Direction[] AMPLIFIER_CORNER_FACINGS = {
        Direction.NORTH,
        Direction.EAST,
        Direction.WEST,
        Direction.SOUTH
    };

    private record RenderEntry(BlockPos pos, BlockState state) {
    }

    public static void offerMissingAmplifierAnvil(BlockPos anvilPos) {
        LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.put(
            anvilPos.immutable(),
            Util.getMillis() + LargeBlockPlacePreviewEventListener.MISSING_AMPLIFIER_PREVIEW_DURATION_MS
        );
    }

    public static void removeMissingAmplifierAnvil(BlockPos anvilPos) {
        LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.remove(anvilPos);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.clear();
        }
    }

    private static void updatePreview() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.level == null) {
            return;
        }
        LargeBlockPlacePreviewEventListener.boundColor = 0xffffffff;
        if (LargeBlockPlacePreviewEventListener.failBoundCooldown > 0) {
            LargeBlockPlacePreviewEventListener.failBoundCooldown--;
            LargeBlockPlacePreviewEventListener.animationActuator.execute();
        }
        if (LargeBlockPlacePreviewEventListener.failBoundErrorCooldown > 0) {
            LargeBlockPlacePreviewEventListener.failBoundErrorCooldown--;
        }
        LargeBlockPlacePreviewEventListener.renderEntries.clear();
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
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
            && !PlacementInteractions.allowsPlacement(new UseOnContext(player, hand, hit))) return;
        UseOnContext useContext;
        List<BuildingRodService.Cell> cells = null;
        if (item.getItem() instanceof PlaceInWaterBlockItem && !player.isUnderWater()) {
            // 不在水下时这类物品贴水面放置：useOn() 返回 PASS，实际落点由 use() 用流体射线
            // （Fluid.SOURCE_ONLY）取得。而准星拾取用的是 Fluid.NONE，且水方块 getShape()
            // 为空，水面根本不会出现在 mc.hitResult 里（还可能被前方实体挡成 EntityHitResult），
            // 故这里不依赖 mc.hitResult，按放置逻辑同样的流体射线取落点；
            // 并逐个尝试 use() 会尝试的候选格，取第一个能放下的，与实际放置保持一致。
            // 水下则退化为下面的普通方块放置预览。
            useContext = null;
            for (UseOnContext candidate :
                PlaceInWaterBlockItem.surfaceCandidates(mc.level, player, hand)) {
                List<BuildingRodService.Cell> attempt = BuildingRodService.singlePlacement(candidate);
                if (!attempt.isEmpty()) {
                    useContext = candidate;
                    cells = attempt;
                    break;
                }
            }
            if (useContext == null) {
                return;
            }
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
                useContext = BlockPlacementPicking.forPlacement(useContext);
            }
        }
        if (useContext instanceof BlockPlacementPicking.PlayerClick click && !click.anvilcraft$hasBlockHit()) {
            return;
        }
        if (cells == null) {
            cells = BuildingRodService.singlePlacement(useContext);
        }
        if (cells.isEmpty()) return;
        LargeBlockPlacePreviewEventListener.validateCanRender(item, blockItem, cells.getFirst().pos());
        for (var cell : cells) {
            LargeBlockPlacePreviewEventListener.renderEntries.add(new RenderEntry(cell.pos(), cell.state()));
        }
    }

    /**
     * 该方块是否参与放置预览：多方块方块，或 {@link ModBlockTags#PLACEMENT_PREVIEW} 内的单方块。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isPreviewable(Block block) {
        return block instanceof AbstractMultiPartBlock<?>
               || block.defaultBlockState().is(ModBlockTags.PLACEMENT_PREVIEW);
    }

    private static void expandRenderEntriesForGhost() {
        RenderEntry base = LargeBlockPlacePreviewEventListener.renderEntries.getFirst();
        if (!(base.state().getBlock() instanceof AbstractMultiPartBlock<?> block)) {
            return;
        }
        ObjectArrayList<RenderEntry> parts = new ObjectArrayList<>();
        for (Enum<?> part : block.getParts()) {
            BlockPos partPos = base.pos().offset(block.offsetFrom(base.state(), LargeBlockPlacePreviewEventListener.cast(part)));
            parts.add(new RenderEntry(partPos, block.placedState(LargeBlockPlacePreviewEventListener.cast(part), base.state())));
        }
        LargeBlockPlacePreviewEventListener.renderEntries.clear();
        LargeBlockPlacePreviewEventListener.renderEntries.addAll(parts);
    }

    @SubscribeEvent
    public static void renderGhost(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.level == null) {
            LargeBlockPlacePreviewEventListener.renderEntries.clear();
            LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.clear();
            return;
        }
        LargeBlockPlacePreviewEventListener.renderMissingAmplifierGhosts(event);
        if (BuildingRodItem.isHeld(player)) {
            LargeBlockPlacePreviewEventListener.renderEntries.clear();
            return;
        }
        if (!AnvilCraftClient.CONFIG.effects.multiPartPreviewMode.isEnabled()) {
            LargeBlockPlacePreviewEventListener.renderEntries.clear();
            return;
        }
        LargeBlockPlacePreviewEventListener.updatePreview();
        if (LargeBlockPlacePreviewEventListener.renderEntries.isEmpty()) {
            return;
        }
        ItemStack item = player.getInventory().getItem(player.getInventory().selected);
        if (!(item.getItem() instanceof BlockItem)) {
            item = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!(item.getItem() instanceof BlockItem blockItem) || !LargeBlockPlacePreviewEventListener.isPreviewable(blockItem.getBlock())) {
            LargeBlockPlacePreviewEventListener.renderEntries.clear();
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        if (AnvilCraftClient.CONFIG.effects.multiPartPreviewMode.isOutline()) {
            if (LargeBlockPlacePreviewEventListener.renderOutline(poseStack, bufferSource, event.getCamera())) {
                return;
            }
            LargeBlockPlacePreviewEventListener.expandRenderEntriesForGhost();
        }
        RenderType renderType = ModRenderTypes.BEACON_GLASS;
        float alpha = (float) AnvilCraftClient.CONFIG.effects.multiPartPreviewGhostOpacity;
        int color = LargeBlockPlacePreviewEventListener.boundColor;
        float red = FastColor.ARGB32.red(color) / 255f;
        float green = FastColor.ARGB32.green(color) / 255f;
        float blue = FastColor.ARGB32.blue(color) / 255f;
        for (RenderEntry entry : LargeBlockPlacePreviewEventListener.renderEntries) {
            poseStack.pushPose();
            poseStack.translate(
                entry.pos().getX() - camera.x - 0.0005,
                entry.pos().getY() - camera.y - 0.0005,
                entry.pos().getZ() - camera.z - 0.0005
            );
            poseStack.scale(1.001f, 1.001f, 1.001f);
            LargeBlockPlacePreviewEventListener.renderPart(poseStack, bufferSource, renderType, entry.state(), alpha, red, green, blue);
            poseStack.popPose();
        }
        // 方块实体模型（如智能方块放置器的机械臂）不属于方块模型，按各自位姿单独渲染
        RenderEntry base = LargeBlockPlacePreviewEventListener.renderEntries.getFirst();
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
            LargeBlockPlacePreviewEventListener.renderModel(
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
        LargeBlockPlacePreviewEventListener.renderErrorBound(poseStack, bufferSource, event.getCamera());
        bufferSource.endBatch(renderType);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderMissingAmplifierGhosts(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.clear();
            return;
        }
        long now = Util.getMillis();
        LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.entrySet().removeIf(
            entry -> now >= entry.getValue()
                     || !(level.getBlockEntity(entry.getKey()) instanceof CelestialForgingAnvilBlockEntity anvil)
                     || anvil.isRemoved()
                     || anvil.isAmplifierPresent()
        );
        if (LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        CelestialForgingAnvilAmplifierBlock amplifier = ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get();
        boolean outlineMode = !AnvilCraftClient.CONFIG.effects.multiPartPreviewMode.isGhost();
        RenderType renderType = outlineMode ? RenderType.lines() : ModRenderTypes.BEACON_GLASS;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        if (outlineMode) {
            LargeBlockPlacePreviewEventListener.renderMissingAmplifierOutlines(poseStack, vertexConsumer, cameraPos, amplifier, level);
        } else {
            LargeBlockPlacePreviewEventListener.renderMissingAmplifierGlass(
                poseStack,
                bufferSource,
                renderType,
                cameraPos,
                amplifier,
                level
            );
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
        for (BlockPos anvilPos : LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.keySet()) {
            for (int i = 0; i < LargeBlockPlacePreviewEventListener.AMPLIFIER_CORNER_OFFSETS.length; i++) {
                BlockPos mainPos = anvilPos.offset(LargeBlockPlacePreviewEventListener.AMPLIFIER_CORNER_OFFSETS[i]);
                if (level.getBlockState(mainPos).is(amplifier)) {
                    continue;
                }
                BlockState state = amplifier.defaultBlockState()
                    .setValue(CelestialForgingAnvilAmplifierBlock.FACING, LargeBlockPlacePreviewEventListener.AMPLIFIER_CORNER_FACINGS[i]);
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
                    OutlineRenderer.render(
                        poseStack, vertexConsumer,
                        CubeSelection.outlines().get(selectionPart.geometry()), 1.0f, 1.0f, 1.0f,
                        (float) AnvilCraftClient.CONFIG.effects.multiPartPreviewOutlineOpacity
                    );
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
        for (BlockPos anvilPos : LargeBlockPlacePreviewEventListener.missingAmplifierAnvilPositions.keySet()) {
            for (int i = 0; i < LargeBlockPlacePreviewEventListener.AMPLIFIER_CORNER_OFFSETS.length; i++) {
                BlockPos mainPos = anvilPos.offset(LargeBlockPlacePreviewEventListener.AMPLIFIER_CORNER_OFFSETS[i]);
                if (level.getBlockState(mainPos).is(amplifier)) {
                    continue;
                }
                BlockState state = amplifier.defaultBlockState()
                    .setValue(CelestialForgingAnvilAmplifierBlock.FACING, LargeBlockPlacePreviewEventListener.AMPLIFIER_CORNER_FACINGS[i]);
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
                    LargeBlockPlacePreviewEventListener.renderPart(
                        poseStack, bufferSource, renderType, partState,
                        (float) AnvilCraftClient.CONFIG.effects.multiPartPreviewGhostOpacity, 1.0f, 1.0f, 1.0f
                    );
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
        if (LargeBlockPlacePreviewEventListener.renderEntries.isEmpty()) {
            return false;
        }
        RenderEntry base = LargeBlockPlacePreviewEventListener.renderEntries.getFirst();
        List<RenderEntry> entries =
            base.state().getBlock() instanceof AbstractMultiPartBlock<?>
            ? List.of(base)
            : LargeBlockPlacePreviewEventListener.renderEntries;
        Map<RenderEntry, List<SelectionPart>> outlines = new LinkedHashMap<>();
        for (RenderEntry entry : entries) {
            List<SelectionPart> outline = new ArrayList<>(ModelBlockSelection.multipartOutline(entry.state()));
            outline.addAll(ModelBlockSelection.previewBerParts(entry.state(), entry.pos()));
            if (outline.isEmpty()) return false;
            outlines.put(entry, outline);
        }
        Vec3 cameraPos = camera.getPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        int color = LargeBlockPlacePreviewEventListener.boundColor;
        float red = FastColor.ARGB32.red(color) / 255f;
        float green = FastColor.ARGB32.green(color) / 255f;
        float blue = FastColor.ARGB32.blue(color) / 255f;
        for (var entry : outlines.entrySet()) {
            BlockPos pos = entry.getKey().pos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            for (SelectionPart part : entry.getValue()) {
                poseStack.pushPose();
                part.apply(poseStack);
                OutlineRenderer.render(
                    poseStack, vertexConsumer,
                    CubeSelection.outlines().get(part.geometry()), red, green, blue,
                    (float) AnvilCraftClient.CONFIG.effects.multiPartPreviewOutlineOpacity
                );
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        LargeBlockPlacePreviewEventListener.renderErrorBound(poseStack, bufferSource, camera);
        bufferSource.endBatch(RenderType.lines());
        return true;
    }

    private static void renderErrorBound(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Camera camera
    ) {
        if (LargeBlockPlacePreviewEventListener.failBoundErrorCooldown <= 0) {
            return;
        }
        Vec3 position = camera.getPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        for (BlockPos blockPos : LargeBlockPlacePreviewEventListener.cachedErrorPosList) {
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
        LargeBlockPlacePreviewEventListener.renderModel(
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
        if (LargeBlockPlacePreviewEventListener.currentItem.isEmpty()) {
            LargeBlockPlacePreviewEventListener.currentItem = item.copy();
        } else if (!LargeBlockPlacePreviewEventListener.currentItem.is(blockItem)) {
            LargeBlockPlacePreviewEventListener.currentItem = ItemStack.EMPTY;
            LargeBlockPlacePreviewEventListener.failBoundCooldown = 0;
        }
        if (LargeBlockPlacePreviewEventListener.currentPos == null) {
            LargeBlockPlacePreviewEventListener.currentPos = pos;
        } else if (!LargeBlockPlacePreviewEventListener.currentPos.equals(pos)) {
            LargeBlockPlacePreviewEventListener.currentPos = null;
            LargeBlockPlacePreviewEventListener.failBoundCooldown = 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static <P extends Enum<P>> P cast(Enum<?> e) {
        return (P) e;
    }

    public static void startFailBoundCooldown() {
        LargeBlockPlacePreviewEventListener.failBoundCooldown = 80;
        LargeBlockPlacePreviewEventListener.animationActuator.reset();
    }

    public static void startFailBoundErrorCooldown(List<BlockPos> errorPosList) {
        LargeBlockPlacePreviewEventListener.failBoundErrorCooldown = 60;
        LargeBlockPlacePreviewEventListener.cachedErrorPosList = new ObjectArrayList<>(errorPosList);
    }
}
