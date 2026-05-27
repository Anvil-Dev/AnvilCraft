package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Axis;
import com.mojang.math.MatrixUtil;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.VertexConsumerWithPose;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderSupport {
    private static final int MAX_CACHE_SIZE = 64;
    private static final LinkedHashMap<BlockState, BlockEntity> BLOCK_ENTITY_CACHE = new LinkedHashMap<>();
    private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();
    public static final Vector3f L1 = new Vector3f(0.4F, 0.0F, 1.0F).normalize();
    public static final Vector3f L2 = new Vector3f(-0.4F, 1.0F, -0.2F).normalize();

    private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.inventory(
        ResourceLocation.withDefaultNamespace("trident")
    );
    private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.inventory(
        ResourceLocation.withDefaultNamespace("spyglass")
    );
    private static ClientLevel currentClientLevel = null;
    private static LevelLike.AirLevelLike airLevelLike = null;

    public static final BlockRenderFunction SINGLE_BLOCK = (blockState, poseStack, buffers) -> {
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blockRenderDispatcher.getBlockModel(blockState);
        for (RenderType renderType : model.getRenderTypes(blockState, RANDOM, ModelData.EMPTY)) {
            VertexConsumer bufferBuilder = buffers.getBuffer(renderType);
            blockRenderDispatcher.renderBatched(
                blockState,
                BlockPos.ZERO,
                airLevelLike,
                poseStack,
                bufferBuilder,
                true,
                RANDOM,
                ModelData.EMPTY,
                renderType
            );
        }
        if (currentClientLevel == null) return;
        getCachedBlockEntity(blockState).ifPresent(blockEntity -> {
            blockEntity.setLevel(currentClientLevel);
            blockEntity.setBlockState(blockState);
            renderBlockEntity(blockEntity, poseStack, buffers);
        });
    };

    public static void renderBlockWithRotationNoTranslate(
        GuiGraphics guiGraphics,
        BlockState block,
        BlockRenderFunction fn
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ClientLevel level = Minecraft.getInstance().level;
        if (currentClientLevel != level) {
            airLevelLike = new LevelLike.AirLevelLike(level);
            currentClientLevel = level;
        }
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        final FluidState fluidState = block.getFluidState();
        MultiBufferSource.BufferSource buffers =
            Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.setupGui3DDiffuseLighting(L1, L2);
        fn.renderBlock(block, poseStack, buffers);
        buffers.endLastBatch();
        if (!fluidState.isEmpty()) {
            if (block.getBlock() instanceof LiquidBlock) {
                block = block.setValue(LiquidBlock.LEVEL, block.getFluidState().getAmount());
            }
            BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
            blockRenderDispatcher.renderLiquid(
                BlockPos.ZERO,
                airLevelLike,
                new VertexConsumerWithPose(
                    buffers.getBuffer(ItemBlockRenderTypes.getRenderLayer(fluidState)),
                    poseStack.last(),
                    BlockPos.ZERO
                ),
                block,
                fluidState
            );
            buffers.endLastBatch();
        }

        poseStack.popPose();
    }

    public static void renderBlockWithRotation(
        GuiGraphics guiGraphics,
        BlockState block,
        float x,
        float y,
        float z,
        float scale,
        BlockRenderFunction fn,
        Quaternionf rotationX,
        Quaternionf rotationY
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ClientLevel level = Minecraft.getInstance().level;
        if (currentClientLevel != level) {
            airLevelLike = new LevelLike.AirLevelLike(level);
            currentClientLevel = level;
        }
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(-scale, -scale, -scale);
        poseStack.translate(-0.5f, -0.5f, 0);
        poseStack.mulPose(rotationX);
        poseStack.translate(0.5F, 0, -0.5F);
        poseStack.mulPose(rotationY);
        poseStack.translate(-0.5F, 0, 0.5F);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        poseStack.translate(0, 0, -1);

        final FluidState fluidState = block.getFluidState();
        MultiBufferSource.BufferSource buffers =
            Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.setupGui3DDiffuseLighting(L1, L2);
        fn.renderBlock(block, poseStack, buffers);
        buffers.endLastBatch();
        if (!fluidState.isEmpty()) {
            if (block.getBlock() instanceof LiquidBlock) {
                block = block.setValue(LiquidBlock.LEVEL, block.getFluidState().getAmount());
            }
            BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
            blockRenderDispatcher.renderLiquid(
                BlockPos.ZERO,
                airLevelLike,
                new VertexConsumerWithPose(
                    buffers.getBuffer(ItemBlockRenderTypes.getRenderLayer(fluidState)),
                    poseStack.last(),
                    BlockPos.ZERO
                ),
                block,
                fluidState
            );
            buffers.endLastBatch();
        }

        poseStack.popPose();
    }

    public static void renderBlock(
        GuiGraphics guiGraphics,
        BlockState block,
        float x,
        float y,
        float z,
        float scale,
        BlockRenderFunction fn
    ) {
        renderBlockWithRotation(
            guiGraphics,
            block,
            x,
            y,
            z,
            scale,
            fn,
            Axis.XP.rotationDegrees(-30F),
            Axis.YP.rotationDegrees(45f)
        );
    }

    public static void renderLevelLike(
        LevelLike level,
        GuiGraphics guiGraphics,
        int posX,
        int posY,
        float scaleFactor,
        float rotationSpeed
    ) {
        RenderSystem.enableBlend();
        Minecraft minecraft = Minecraft.getInstance();
        DeltaTracker tracker = minecraft.getTimer();
        ClientLevel clientLevel = minecraft.level;
        PoseStack pose = guiGraphics.pose();
        int sizeX = level.horizontalSize();
        int sizeY = level.verticalSize();

        pose.pushPose();
        pose.translate(posX, posY, 100);
        float scaleX = scaleFactor / (sizeX * Mth.SQRT_OF_TWO);
        float scaleY = scaleFactor / (float) sizeY;
        float scale = Math.min(scaleY, scaleX);
        pose.scale(-scale, -scale, -scale);

        // 奇数尺寸时旋转中心偏移0.5，使 block 居中对齐
        float centerOffset = ((sizeX + 1) % 2 != 0) ? -0.5f : 0.0f;
        pose.translate(-(float) sizeX / 2 + centerOffset, -(float) sizeY / 2, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-30));

        float offsetX = (float) -sizeX / 2 + centerOffset;
        float offsetZ = (float) -sizeX / 2 + 1 + centerOffset;
        float rotationY = (clientLevel.getGameTime() + tracker.getGameTimeDeltaPartialTick(true)) * rotationSpeed;

        pose.translate(-offsetX, 0, -offsetZ);
        pose.mulPose(Axis.YP.rotationDegrees(rotationY + 45));

        pose.translate(offsetX, 0, offsetZ);

        Iterable<BlockPos> iter;
        if (level.isAllLayersVisible()) {
            iter = BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(sizeX - 1, sizeY - 1, sizeX - 1));
        } else {
            int visibleLayer = level.getCurrentVisibleLayer();
            iter = BlockPos.betweenClosed(
                BlockPos.ZERO.atY(visibleLayer), new BlockPos(sizeX - 1, visibleLayer, sizeX - 1));
        }
        pose.pushPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.translate(0, 0, -1);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        for (BlockPos pos : iter) {
            BlockState state = level.getBlockState(pos);
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluid);
                VertexConsumer vertex = buffers.getBuffer(renderType);
                blockRenderer.renderLiquid(pos, level, new VertexConsumerWithPose(vertex, pose.last(), pos), state, fluid);
            }
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                BakedModel bakedModel = blockRenderer.getBlockModel(state);
                for (RenderType type : bakedModel.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
                    VertexConsumer vertex = buffers.getBuffer(type);
                    blockRenderer.renderBatched(state, pos, level, pose, vertex, false, RANDOM, ModelData.EMPTY, type);
                }
            }

            Optional.ofNullable(level.getBlockEntity(pos))
                .ifPresent(blockEntity -> renderBlockEntity(blockEntity, pose, buffers));
            pose.popPose();
        }
        buffers.endBatch();
        pose.popPose();
        pose.popPose();
    }

    public static void renderLevelLike(
        LevelLike level,
        GuiGraphics guiGraphics,
        int posX,
        int posY,
        float scale
    ) {
        renderLevelLike(level, guiGraphics, posX, posY, scale, 0.0f);
    }
    
    /**
     * 渲染LevelLike，支持固定旋转角度和固定尺寸
     *
     * @param rotationX X轴旋转角度（度）
     * @param rotationY Y轴旋转角度（度）
     * @param fixedSizeX 固定的X尺寸
     * @param fixedSizeY 固定的Y尺寸
     */
    @SuppressWarnings({
        "checkstyle:VariableDeclarationUsageDistance",
        "checkstyle:RequireEmptyLineBeforeBlockTagGroup"
    })
    public static void renderLevelLikeWithFixedSize(
        LevelLike level,
        GuiGraphics guiGraphics,
        int posX,
        int posY,
        float scaleFactor,
        float rotationX,
        float rotationY,
        int fixedSizeX,
        int fixedSizeY,
        float centerOffset
    ) {
        RenderSystem.enableBlend();
        final Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = guiGraphics.pose();

        pose.pushPose();
        pose.translate(posX, posY, 100);
        float scaleX = scaleFactor / (fixedSizeX * Mth.SQRT_OF_TWO);
        float scaleY = scaleFactor / (float) fixedSizeY;
        float scale = Math.min(scaleY, scaleX);
        pose.scale(-scale, -scale, -scale);

        // 奇数尺寸时旋转中心偏移0.5，使 block 居中对齐
        centerOffset += (fixedSizeX % 2 != 0) ? 0.5f : 0.0f;
        pose.translate(-(float) fixedSizeX / 2 + centerOffset, -(float) fixedSizeY / 2, 0);

        // 先应用X轴旋转（俯视角度）
        pose.mulPose(Axis.XP.rotationDegrees(rotationX));

        // Y轴旋转中心：固定基于5x5范围计算，忽略放置器
        float offsetX = (float) -fixedSizeX / 2 + centerOffset;
        float offsetZ = (float) -fixedSizeX / 2 + 1 + centerOffset;
        
        // 再应用Y轴旋转（水平旋转）
        pose.translate(-offsetX, 0, -offsetZ);
        pose.mulPose(Axis.YP.rotationDegrees(rotationY + 45));
        pose.translate(offsetX, 0, offsetZ);

        Iterable<BlockPos> iter;
        if (level.isAllLayersVisible()) {
            // 使用LevelLike的实际尺寸，但确保至少包含fixedSize范围
            int actualSizeX = Math.max(fixedSizeX, level.horizontalSize());
            int actualSizeY = Math.max(fixedSizeY, level.verticalSize());
            iter = BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(actualSizeX - 1, actualSizeY - 1, actualSizeX - 1));
        } else {
            int visibleLayer = level.getCurrentVisibleLayer();
            int actualSizeX = Math.max(fixedSizeX, level.horizontalSize());
            // 单层模式：遍历可见层 + 始终渲染的方块所在的层
            int minLayer = Math.min(0, visibleLayer);  // 包含Y=0（放置器）
            int maxLayer = Math.max(visibleLayer, level.getAlwaysRenderBlocks().stream()
                .mapToInt(BlockPos::getY)
                .max()
                .orElse(visibleLayer));
            iter = BlockPos.betweenClosed(
                BlockPos.ZERO.atY(minLayer), new BlockPos(actualSizeX - 1, maxLayer, actualSizeX - 1));
        }
        pose.pushPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.translate(0, 0, -1);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        for (BlockPos pos : iter) {
            BlockState state = level.getBlockState(pos);
            
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluid);
                VertexConsumer vertex = buffers.getBuffer(renderType);
                blockRenderer.renderLiquid(pos, level, new VertexConsumerWithPose(vertex, pose.last(), pos), state, fluid);
            }
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                BakedModel bakedModel = blockRenderer.getBlockModel(state);
                for (RenderType type : bakedModel.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
                    VertexConsumer vertex = buffers.getBuffer(type);
                    blockRenderer.renderBatched(state, pos, level, pose, vertex, false, RANDOM, ModelData.EMPTY, type);
                }
            }

            Optional.ofNullable(level.getBlockEntity(pos))
                .ifPresent(blockEntity -> renderBlockEntity(blockEntity, pose, buffers));
            pose.popPose();
        }
        buffers.endBatch();
        pose.popPose();
        pose.popPose();
    }

    private static Optional<BlockEntity> getCachedBlockEntity(BlockState state) {
        if (!state.hasBlockEntity()) return Optional.empty();
        if (BLOCK_ENTITY_CACHE.containsKey(state)) return Optional.of(BLOCK_ENTITY_CACHE.get(state));
        Optional<BlockEntity> opt = Optional.of(state.getBlock())
            .filter(b -> b instanceof EntityBlock)
            .map(b -> ((EntityBlock) b).newBlockEntity(BlockPos.ZERO, state));
        opt.ifPresent(be -> {
            BLOCK_ENTITY_CACHE.put(state, be);
            if (BLOCK_ENTITY_CACHE.size() > MAX_CACHE_SIZE) {
                BLOCK_ENTITY_CACHE.pollFirstEntry();
            }
        });
        return opt;
    }

    private static void renderBlockEntity(
        BlockEntity blockEntity,
        PoseStack pose,
        MultiBufferSource.BufferSource buffers) {
        BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher().getRenderer(blockEntity);
        if (renderer == null) return;
        try {
            renderer.render(blockEntity, getPartialTick(), pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        } catch (Exception ignored) {
            // do nothing
        }
    }

    public static void renderItemWithTransparency(ItemStack stack, PoseStack poseStack, int x, int y, float alpha) {
        renderItemWithTransparency(Minecraft.getInstance().player, Minecraft.getInstance().level, poseStack, stack, x, y, alpha);
    }

    private static void renderItemWithTransparency(
        @Nullable LivingEntity entity, @Nullable Level level, PoseStack pose, ItemStack stack, int x, int y, float alpha
    ) {
        if (!stack.isEmpty()) {
            BakedModel bakedmodel = Minecraft.getInstance().getItemRenderer().getModel(stack, level, entity, 0);
            pose.pushPose();
            pose.translate((float) (x + 8), (float) (y + 8), (float) (150));

            try {
                pose.scale(16.0F, -16.0F, 16.0F);
                boolean flag = !bakedmodel.usesBlockLight();
                if (flag) {
                    Lighting.setupForFlatItems();
                }


                renderItemStackWithTransparency(
                    Minecraft.getInstance()
                        .getItemRenderer(),
                    stack,
                    ItemDisplayContext.GUI,
                    false,
                    pose,
                    Minecraft.getInstance().levelRenderer.renderBuffers.bufferSource(),
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    bakedmodel,
                    alpha
                );
                if (flag) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                crashreportcategory.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
                crashreportcategory.setDetail("Item Components", () -> String.valueOf(stack.getComponents()));
                crashreportcategory.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
                throw new ReportedException(crashreport);
            }

            pose.popPose();
        }
    }

    public static void renderItemStackWithTransparency(
        ItemRenderer itemRenderer,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        BakedModel bakedModel,
        float alpha
    ) {
        if (!itemStack.isEmpty()) {
            poseStack.pushPose();
            boolean flag = displayContext == ItemDisplayContext.GUI
                           || displayContext == ItemDisplayContext.GROUND
                           || displayContext == ItemDisplayContext.FIXED;
            if (flag) {
                if (itemStack.is(Items.TRIDENT)) {
                    bakedModel = itemRenderer.getItemModelShaper().getModelManager().getModel(TRIDENT_MODEL);
                } else if (itemStack.is(Items.SPYGLASS)) {
                    bakedModel = itemRenderer.getItemModelShaper().getModelManager().getModel(SPYGLASS_MODEL);
                }
            }

            bakedModel = net.neoforged.neoforge.client.ClientHooks.handleCameraTransforms(poseStack, bakedModel, displayContext, leftHand);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            if (!bakedModel.isCustomRenderer() && (!itemStack.is(Items.TRIDENT) || flag)) {
                boolean flag1;
                if (
                    displayContext != ItemDisplayContext.GUI
                    && !displayContext.firstPerson()
                    && itemStack.getItem() instanceof BlockItem blockitem
                ) {
                    Block block = blockitem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                } else {
                    flag1 = true;
                }

                for (BakedModel model : bakedModel.getRenderPasses(itemStack, flag1)) {
                    for (RenderType rendertype : model.getRenderTypes(itemStack, flag1)) {
                        VertexConsumer vertexconsumer;
                        if (hasAnimatedTexture(itemStack) && itemStack.hasFoil()) {
                            PoseStack.Pose pose = poseStack.last().copy();
                            if (displayContext == ItemDisplayContext.GUI) {
                                MatrixUtil.mulComponentWise(pose.pose(), 0.5F);
                            } else if (displayContext.firstPerson()) {
                                MatrixUtil.mulComponentWise(pose.pose(), 0.75F);
                            }

                            vertexconsumer = getCompassFoilBuffer(bufferSource, rendertype, pose);
                        } else {
                            if (flag1) {
                                vertexconsumer = getFoilBufferDirect(bufferSource, rendertype, true, itemStack.hasFoil());
                            } else {
                                vertexconsumer = getFoilBuffer(bufferSource, rendertype, true, itemStack.hasFoil());
                            }
                        }

                        renderModelListsWithTransparency(
                            itemRenderer,
                            model,
                            itemStack,
                            combinedLight,
                            combinedOverlay,
                            poseStack,
                            vertexconsumer,
                            alpha
                        );
                    }
                }
            } else {
                IClientItemExtensions.of(itemStack)
                    .getCustomRenderer()
                    .renderByItem(itemStack, displayContext, poseStack, bufferSource, combinedLight, combinedOverlay);
            }

            poseStack.popPose();
        }
    }

    public static RenderType useTranslucentIfPossible(RenderType original) {
        if (original instanceof RenderType.CompositeRenderType compositeRenderType) {
            if (compositeRenderType.state().transparencyState == RenderStateShard.NO_TRANSPARENCY
                && compositeRenderType.state().textureState instanceof RenderStateShard.TextureStateShard textureStateShard
            ) {
                Optional<ResourceLocation> text = textureStateShard.texture;
                if (text.isPresent()) {
                    return RenderType.entityTranslucentCull(text.get());
                }
            }
        }
        return original;
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose) {
        return VertexMultiConsumer.create(
            new SheetedDecalTextureGenerator(bufferSource.getBuffer(RenderType.glint()), pose, 0.0078125F),
            bufferSource.getBuffer(useTranslucentIfPossible(renderType))
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource source, RenderType type, boolean isItem, boolean glint) {
        if (glint) {
            return Minecraft.useShaderTransparency() && type == Sheets.translucentItemSheet()
                ? VertexMultiConsumer.create(
                    source.getBuffer(RenderType.glintTranslucent()),
                    source.getBuffer(useTranslucentIfPossible(type)))
                : VertexMultiConsumer.create(
                    source.getBuffer(isItem ? RenderType.glint() : RenderType.entityGlint()),
                    source.getBuffer(useTranslucentIfPossible(type)));
        } else {
            return source.getBuffer(type);
        }
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource source, RenderType type, boolean noEntity, boolean withGlint) {
        return withGlint
            ? VertexMultiConsumer.create(
                source.getBuffer(noEntity ? RenderType.glint() : RenderType.entityGlintDirect()),
                source.getBuffer(useTranslucentIfPossible(type)))
            : source.getBuffer(useTranslucentIfPossible(type));
    }

    public static void renderModelListsWithTransparency(
        ItemRenderer renderer,
        BakedModel model,
        ItemStack stack,
        int combinedLight,
        int combinedOverlay,
        PoseStack pose,
        VertexConsumer buffer,
        float alpha
    ) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.values()) {
            randomsource.setSeed(i);
            renderQuadListWithTransparency(
                renderer,
                pose,
                buffer,
                model.getQuads(null, direction, randomsource),
                stack,
                combinedLight,
                combinedOverlay,
                alpha
            );
        }

        randomsource.setSeed(i);
        renderQuadListWithTransparency(
            renderer,
            pose,
            buffer,
            model.getQuads(null, null, randomsource),
            stack,
            combinedLight,
            combinedOverlay,
            alpha
        );
    }

    private static void renderQuadListWithTransparency(
        ItemRenderer renderer,
        PoseStack poseStack,
        VertexConsumer buffer,
        List<BakedQuad> quads,
        ItemStack stack,
        int combinedLight,
        int combinedOverlay,
        float alpha
    ) {
        boolean flag = !stack.isEmpty();
        PoseStack.Pose pose = poseStack.last();

        for (BakedQuad bakedquad : quads) {
            int i = -1;
            if (flag && bakedquad.isTinted()) {
                i = renderer.itemColors.getColor(stack, bakedquad.getTintIndex());
            }

            float f1 = (float) FastColor.ARGB32.red(i) / 255.0F;
            float f2 = (float) FastColor.ARGB32.green(i) / 255.0F;
            float f3 = (float) FastColor.ARGB32.blue(i) / 255.0F;
            // Neo: pass readExistingColor=true
            buffer.putBulkData(pose, bakedquad, f1, f2, f3, alpha, combinedLight, combinedOverlay, true);
        }
    }

    private static boolean hasAnimatedTexture(ItemStack stack) {
        return stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK);
    }

    public static float getPartialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(Minecraft.getInstance().isPaused());
    }

    @FunctionalInterface
    public interface BlockRenderFunction {
        void renderBlock(BlockState block, PoseStack poseStack, MultiBufferSource.BufferSource buffers);
    }
}
