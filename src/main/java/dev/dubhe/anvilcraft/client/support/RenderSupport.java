package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.WipBlockEntityRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.BiConsumer;

// TODO:
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderSupport {
    public static void renderItemWithTransparency(net.minecraft.world.item.ItemStack stack, GuiGraphicsExtractor graphics,
                                                  int x, int y, float alpha) {
        TransparentItemRenderer.extract(stack, graphics, x, y, alpha);
    }

    public static void renderSlotGhost(net.minecraft.world.item.ItemStack stack, GuiGraphicsExtractor graphics,
                                       int x, int y, float alpha, int overlay) {
        TransparentItemRenderer.extract(stack, graphics, x, y, alpha, overlay);
    }

    private static final int MAX_CACHE_SIZE = 64;
    private static final float WIP_PREVIEW_SCALE = 0.6F;
    private static final LinkedHashMap<BlockState, BlockEntity> BLOCK_ENTITY_CACHE = new LinkedHashMap<>();
    private static final LinkedHashMap<WipPreviewKey, LevelLike> WIP_LEVEL_CACHE = new LinkedHashMap<>();
    private static final LinkedHashMap<PreviewModelKey, BlockModelRenderState> PREVIEW_MODELS = new LinkedHashMap<>();
    // private static final RandomSource RANDOM = RandomSource.createThreadLocalInstance();
    // public static final Vector3f L1 = new Vector3f(0.4F, 0.0F, 1.0F).normalize();
    // public static final Vector3f L2 = new Vector3f(-0.4F, 1.0F, -0.2F).normalize();
    private static final PoseStack.Pose BLOCK_DISPLAY_POSE;
    private static @Nullable ClientLevel currentClientLevel;

    static {
        BLOCK_DISPLAY_POSE = new PoseStack.Pose();
        RenderSupport.BLOCK_DISPLAY_POSE.rotate(Axis.XP.rotationDegrees(30));
        RenderSupport.BLOCK_DISPLAY_POSE.rotate(Axis.YP.rotationDegrees(45));
    }

    public static int processAnchorIndex(AbstractProcessRecipe<?> recipe) {
        double inputY = recipe.getProperty().getBlockInputOffset().y;
        double outputY = recipe.getProperty().getBlockOutputOffset().y;
        int index = (int) Math.round(inputY - outputY);
        return Math.clamp(index, 0, Math.max(recipe.getInputBlocks().size() - 1, 0));
    }

    public static void renderBlock(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float size) {
        GuiRenderExtras.tessellateBlock(
            graphics,
            block,
            null,
            null,
            x,
            y,
            x + size,
            y + size,
            -1,
            true,
            RenderSupport.BLOCK_DISPLAY_POSE.copy()
        );
    }

    /** Draw a preview at an anchor with a fixed number of GUI pixels per block. */
    public static void renderBlockAt(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float scale) {
        if (block.getBlock() instanceof DoorBlock) {
            BlockState closed = block.setValue(DoorBlock.OPEN, false);
            var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
            BlockState lowerBlock = closed.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            BlockState upperBlock = closed.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            final var lower = RenderSupport.previewModel(models.get(lowerBlock), lowerBlock, false);
            final var upper = RenderSupport.previewModel(models.get(upperBlock), upperBlock, false);
            RenderSupport.renderModelsAt(graphics, x, y, scale, (collector, pose) -> {
                pose.pushPose();
                pose.translate(0, -1, 0);
                lower.submitMultiLayer(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
                upper.submitMultiLayer(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            });
        } else if (!RenderSupport.renderBlockEntityAt(graphics, block, x, y, scale)) {
            RenderSupport.renderSingleBlockAt(graphics, block, x, y, scale);
        }
    }

    private static boolean renderBlockEntityAt(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float scale) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        BlockEntity entity = RenderSupport.getCachedBlockEntity(block).orElse(null);
        if (entity == null) return false;
        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return false;
        BlockEntityRenderState state = renderer.createRenderState();
        var camera = client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        entity.setLevel(client.level);
        renderer.extractRenderState(entity, state, client.getDeltaTracker().getGameTimeDeltaPartialTick(true), camera.pos, null);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        BlockModelRenderState model = block.getRenderShape() == RenderShape.MODEL
            ? RenderSupport.previewModel(client.getModelManager().getBlockStateModelSet().get(block), block, false) : null;
        RenderSupport.renderModelsAt(graphics, x, y, scale, (collector, pose) -> {
            if (model != null) model.submitMultiLayer(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            renderer.submit(state, pose, collector, camera);
        });
        return true;
    }

    private static void renderSingleBlockAt(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float scale) {
        float size = 8 * scale;
        float fittedScale = 8 / (1 + (float) Math.sqrt(2) / 2);
        PoseStack.Pose pose = new PoseStack.Pose();
        pose.scale(1 / fittedScale, 1 / fittedScale, 1 / fittedScale);
        pose.rotate(Axis.XP.rotationDegrees(30));
        pose.rotate(Axis.YP.rotationDegrees(225));
        float left = x - size / 2;
        float top = y + scale * (fittedScale * 0.5F - 7.25F);
        float resolution = Math.max(1, Math.max(
            (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11())));
        left *= resolution;
        top *= resolution;
        size *= resolution;
        float alignedTop = (float) Math.floor(top);
        graphics.pose().pushMatrix();
        graphics.pose().scale(1 / resolution, 1 / resolution);
        graphics.pose().translate(0, top - alignedTop);
        GuiRenderExtras.tessellateBlock(graphics, block, null, null, left, alignedTop, left + size, alignedTop + size,
            -1, Minecraft.getInstance().options.ambientOcclusion().get(), pose);
        graphics.pose().popMatrix();
    }

    public static void render3x3Block(GuiGraphicsExtractor graphics, BlockState block, float x, float y, float size) {
        PoseStack.Pose poseStack = RenderSupport.BLOCK_DISPLAY_POSE.copy();
        poseStack.scale(0.3f, 0.3f, 0.3f);
        GuiRenderExtras.tessellateBlock(
            graphics,
            block,
            null,
            null,
            x,
            y,
            x + size,
            y + size,
            -1,
            true,
            poseStack
        );
    }

    public static void renderWipBlock(
        GuiGraphicsExtractor graphics,
        Identifier recipeId,
        int stepCount,
        float x,
        float y,
        float size
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            RenderSupport.renderBlock(graphics, ModBlocks.WIP_BLOCK.get().defaultBlockState(), x, y, size);
            return;
        }
        if (RenderSupport.currentClientLevel != level) {
            RenderSupport.currentClientLevel = level;
            RenderSupport.WIP_LEVEL_CACHE.clear();
        }
        WipPreviewKey key = new WipPreviewKey(recipeId, stepCount);
        if (!RenderSupport.WIP_LEVEL_CACHE.containsKey(key) && RenderSupport.WIP_LEVEL_CACHE.size() >= RenderSupport.MAX_CACHE_SIZE) {
            RenderSupport.WIP_LEVEL_CACHE.pollFirstEntry();
        }
        LevelLike preview = RenderSupport.WIP_LEVEL_CACHE.computeIfAbsent(key, previewKey -> {
            LevelLike result = new LevelLike(level);
            result.setBlockState(BlockPos.ZERO, ModBlocks.WIP_BLOCK.get().defaultBlockState());
            if (result.getBlockEntity(BlockPos.ZERO) instanceof WipBlockEntity wip) {
                wip.setRecipeId(previewKey.recipeId());
                wip.setStepCount(previewKey.stepCount());
            }
            return result;
        });
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(RenderSupport.BLOCK_DISPLAY_POSE);
        GuiRenderExtras.submitStructure(
            graphics,
            preview,
            BlockPos.ZERO,
            BlockPos.ZERO,
            x,
            y,
            x + size,
            y + size,
            size * RenderSupport.WIP_PREVIEW_SCALE,
            true,
            false,
            poseStack
        );
    }

    public static void renderWipBlockAt(
        GuiGraphicsExtractor graphics, @Nullable Identifier displayedModel, float x, float y, float scale
    ) {
        var manager = Minecraft.getInstance().getModelManager();
        var key = displayedModel == null ? null : WipBlockEntityRenderer.getModelKey(displayedModel);
        var body = key == null ? null : manager.getStandaloneModel(key);
        var shell = manager.getBlockStateModelSet().get(ModBlocks.WIP_BLOCK.getDefaultState());
        final BlockModelRenderState bodyState = body == null ? null
            : RenderSupport.previewModel(body, ModBlocks.WIP_BLOCK.getDefaultState(), false);
        final BlockModelRenderState shellState = RenderSupport.previewModel(shell, ModBlocks.WIP_BLOCK.getDefaultState(), true);
        RenderSupport.renderModelsAt(graphics, x, y, scale, (collector, pose) -> {
            if (bodyState != null) bodyState.submitMultiLayer(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            shellState.submitMultiLayer(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        });
    }

    private static void renderModelsAt(
        GuiGraphicsExtractor graphics, float x, float y, float scale, BiConsumer<SubmitNodeCollector, PoseStack> draw
    ) {
        PoseStack pose = RenderSupport.previewPose();
        float resolution = Math.max(1, Math.max(
            (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11())));
        float extent = scale * 4;
        graphics.pose().pushMatrix();
        graphics.pose().scale(1 / resolution, 1 / resolution);
        GuiRenderExtras.submitStructure(graphics, BlockAndTintGetter.EMPTY, BlockPos.ZERO, BlockPos.ZERO,
            (x - extent) * resolution, (y - extent) * resolution, (x + extent) * resolution, (y + extent) * resolution,
            scale * resolution, false, false, pose, draw);
        graphics.pose().popMatrix();
    }

    static PoseStack previewPose() {
        PoseStack pose = new PoseStack();
        pose.scale(-1, 1, -1);
        pose.translate(-0.5, -0.5, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-30));
        pose.translate(0.5, 0, -0.5);
        pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.translate(-0.5, 0, 0.5);
        pose.translate(0.5, 0.5, -0.5);
        return pose;
    }

    private static BlockModelRenderState previewModel(BlockStateModel model, BlockState state, boolean translucent) {
        PreviewModelKey key = new PreviewModelKey(model, state, translucent);
        if (!RenderSupport.PREVIEW_MODELS.containsKey(key) && RenderSupport.PREVIEW_MODELS.size() >= RenderSupport.MAX_CACHE_SIZE) {
            RenderSupport.PREVIEW_MODELS.pollFirstEntry();
        }
        return RenderSupport.PREVIEW_MODELS.computeIfAbsent(key, entry -> {
            BlockModelRenderState result = new BlockModelRenderState();
            entry.model().collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, entry.state(),
                RandomSource.create(42), result.setupModel(new Matrix4f(), entry.translucent()));
            return result;
        });
    }

    private record PreviewModelKey(BlockStateModel model, BlockState state, boolean translucent) {
    }

    /** Draw a recipe preview using the source animation's anchor, scale and rotation. */
    public static void renderLevelLikeAt(
        LevelLike level,
        GuiGraphicsExtractor graphics,
        int posX,
        int posY,
        float scaleFactor,
        float rotationSpeed
    ) {
        var min = level.getMinPos();
        var max = level.getMaxPos();
        var minecraft = Minecraft.getInstance();
        var clientLevel = minecraft.level;
        if (min.isEmpty() || max.isEmpty() || clientLevel == null) return;
        int sizeX = level.horizontalSize();
        int sizeY = level.verticalSize();
        if (sizeX <= 0 || sizeY <= 0) return;
        float scale = Math.min(scaleFactor / (sizeX * Mth.SQRT_OF_TWO), scaleFactor / sizeY);
        float centerOffset = (sizeX + 1) % 2 != 0 ? -0.5F : 0;
        float offsetX = -sizeX / 2F + centerOffset;
        float offsetZ = -sizeX / 2F + 1 + centerOffset;
        float rotation = (clientLevel.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)) * rotationSpeed;
        PoseStack pose = new PoseStack();
        pose.scale(-1, 1, -1);
        pose.translate(-sizeX / 2F + centerOffset, -sizeY / 2F, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-30));
        pose.translate(-offsetX, 0, -offsetZ);
        pose.mulPose(Axis.YP.rotationDegrees(rotation + 45));
        pose.translate(offsetX, 0, offsetZ);
        // StructurePipRenderer subtracts half a block before tessellation.
        pose.translate(0.5F, 0.5F, -0.5F);
        int extent = Mth.ceil(scaleFactor);
        GuiRenderExtras.submitStructure(graphics, level, visibleLayerPos(level, min.get()), visibleLayerPos(level, max.get()),
            posX - extent, posY - extent, posX + extent, posY + extent,
            scale, true, false, pose);
    }

    public static void renderLevelLike(
        LevelLike level,
        GuiGraphicsExtractor graphics,
        int posX,
        int posY,
        int size,
        int scale,
        float rotationSpeed,
        boolean glitched
    ) {
        Optional<BlockPos> minPos = level.getMinPos();
        Optional<BlockPos> maxPos = level.getMaxPos();
        if (minPos.isEmpty() || maxPos.isEmpty()) return;
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(RenderSupport.BLOCK_DISPLAY_POSE);
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel currentLevel = minecraft.level;
        if (currentLevel == null) return;
        float gameTime = currentLevel.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * rotationSpeed));
        poseStack.translate(-(minPos.get().getX() + maxPos.get().getX()) / 2F,
            -(minPos.get().getY() + maxPos.get().getY()) / 2F,
            -(minPos.get().getZ() + maxPos.get().getZ()) / 2F);
        GuiRenderExtras.submitStructure(
            graphics,
            level,
            visibleLayerPos(level, minPos.get()),
            visibleLayerPos(level, maxPos.get()),
            posX,
            posY,
            posX + size,
            posY + size,
            scale,
            true,
            glitched,
            poseStack
        );
    }

    private static BlockPos visibleLayerPos(LevelLike level, BlockPos pos) {
        return level.isAllLayersVisible() ? pos : pos.atY(level.getCurrentVisibleLayer());
    }

    private static Optional<BlockEntity> getCachedBlockEntity(BlockState state) {
        if (!state.hasBlockEntity()) return Optional.empty();
        if (RenderSupport.BLOCK_ENTITY_CACHE.containsKey(state)) return Optional.of(RenderSupport.BLOCK_ENTITY_CACHE.get(state));
        Optional<BlockEntity> opt = Optional.of(state.getBlock())
            .filter(b -> b instanceof EntityBlock)
            .map(b -> ((EntityBlock) b).newBlockEntity(BlockPos.ZERO, state));
        opt.ifPresent(be -> {
            RenderSupport.BLOCK_ENTITY_CACHE.put(state, be);
            if (RenderSupport.BLOCK_ENTITY_CACHE.size() > RenderSupport.MAX_CACHE_SIZE) {
                RenderSupport.BLOCK_ENTITY_CACHE.pollFirstEntry();
            }
        });
        return opt;
    }

    private record WipPreviewKey(Identifier recipeId, int stepCount) {
    }
}
