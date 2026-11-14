package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Axis;
import com.mojang.math.MatrixUtil;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BaseShowItemRenderer;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.VertexConsumerWithPose;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("deprecation")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderSupport {
    private static final RenderType ITEM_SHADOW = RenderType.entityShadow(
        ResourceLocation.withDefaultNamespace("textures/misc/shadow.png")
    );
    private static final Function<RenderType, RenderType> FULL_BRIGHT_ITEM_TYPES = Util.memoize(RenderSupport::fullBrightItemType);
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
    private static @Nullable ClientLevel currentClientLevel = null;
    private static LevelLike.@Nullable AirLevelLike airLevelLike = null;

    public static final BlockRenderFunction SINGLE_BLOCK = (blockState, poseStack, buffers) -> {
        if (airLevelLike == null) return;
        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        if (blockState.getBlock() instanceof DoorBlock) {
            renderDoorBlock(blockRenderDispatcher, blockState, poseStack, buffers);
            return;
        }
        renderBlockState(blockRenderDispatcher, blockState, poseStack, buffers);
        if (currentClientLevel == null) return;
        getCachedBlockEntity(blockState).ifPresent(blockEntity -> {
            blockEntity.setLevel(currentClientLevel);
            blockEntity.setBlockState(blockState);
            renderBlockEntity(blockEntity, poseStack, buffers);
        });
    };

    private static void renderDoorBlock(
        BlockRenderDispatcher blockRenderDispatcher,
        BlockState blockState,
        PoseStack poseStack,
        MultiBufferSource buffers
    ) {
        // 门是双格方块：上半扇锚定原格，下半扇向下叠一格，保持原尺寸完整拼接，利用原格下方的空间避免与上方砧子重叠
        BlockState closedState = blockState.setValue(DoorBlock.OPEN, false);
        poseStack.pushPose();
        poseStack.translate(0.0F, -1.0F, 0.0F);
        renderBlockState(
            blockRenderDispatcher,
            closedState.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
            poseStack,
            buffers
        );
        poseStack.popPose();
        poseStack.pushPose();
        renderBlockState(
            blockRenderDispatcher,
            closedState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
            poseStack,
            buffers
        );
        poseStack.popPose();
    }

    private static void renderBlockState(
        BlockRenderDispatcher blockRenderDispatcher,
        BlockState blockState,
        PoseStack poseStack,
        MultiBufferSource buffers
    ) {
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
    }

    /**
     * 手册 / JEI 等展示场景渲染方块序列加工的中间态（WIP）方块。
     *
     * <p>{@code wip_block} 的外观不由自身 blockstate 决定，而是按配方的 {@code displayedModels}
     * 与当前步数解析出一个独立的 {@code block/wip_display} 模型。若走普通的
     * {@link #SINGLE_BLOCK} 路径，缓存的方块实体没有配方与步数，会退回到初始方块（默认空气）
     * 的模型，因而什么都渲染不出来。</p>
     *
     * <p>展示场景统一点亮，不采样世界光照，与同屏其它方块预览保持一致。</p>
     *
     * @param recipe    所属的序列加工配方，用于按步数取中间态模型
     * @param stepCount 该进程方块已完成的步数；作为第 {@code idx} 步的输入时为 {@code idx}
     */
    public static BlockRenderFunction wipDisplay(ProceduralProcessRecipe recipe, int stepCount) {
        return (blockState, poseStack, buffers) -> {
            ResourceLocation modelId = recipe.getDisplayedModelForStep(stepCount).orElse(null);
            if (modelId == null) return;
            BakedModel model = Minecraft.getInstance().getModelManager()
                .getModel(ModelResourceLocation.standalone(modelId));
            for (RenderType renderType : model.getRenderTypes(blockState, RANDOM, ModelData.EMPTY)) {
                Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(),
                    buffers.getBuffer(renderType),
                    blockState,
                    model,
                    0,
                    0,
                    0,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
                );
            }
        };
    }

    public static void renderBlockWithRotationNoTranslate(
        GuiGraphics guiGraphics,
        BlockState block,
        BlockRenderFunction fn
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ClientLevel level = Minecraft.getInstance().level;
        if (currentClientLevel != level && level != null) {
            airLevelLike = new LevelLike.AirLevelLike(level);
            currentClientLevel = level;
        }
        if (currentClientLevel == null || airLevelLike == null) {
            return;
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
        if (currentClientLevel != level && level != null) {
            airLevelLike = new LevelLike.AirLevelLike(level);
            currentClientLevel = level;
        }
        if (currentClientLevel == null || airLevelLike == null) {
            return;
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
        float scale
    ) {
        renderLevelLike(level, guiGraphics, posX, posY, scale, 0.0f);
    }

    public static void renderLevelLike(LevelLike level, GuiGraphics graphics, int posX, int posY, float scaleFactor, float rotationSpeed) {
        renderLevelLikeAt(level, graphics, posX, posY, scaleFactor, rotationSpeed, (int) Math.ceil(scaleFactor * 2), false);
    }

    /** 保留配方动画的原点和缩放，方块、流体与实体交给砧库绘制。 */
    public static void renderLevelLikeAt(LevelLike level, GuiGraphics graphics, int posX, int posY,
                                        float scaleFactor, float rotationSpeed, int previewSize, boolean scanEffect) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        int sizeX = Math.max(1, level.horizontalSize());
        int sizeY = Math.max(1, level.verticalSize());
        float scale = Math.min(scaleFactor / (sizeX * Mth.SQRT_OF_TWO), scaleFactor / sizeY);
        float centerOffset = (sizeX + 1) % 2 != 0 ? -0.5f : 0;
        float offsetX = -sizeX / 2f + centerOffset;
        float offsetZ = -sizeX / 2f + 1 + centerOffset;
        double rotationY = Mth.positiveModulo(minecraft.level.getGameTime() * (double) rotationSpeed
            + minecraft.getTimer().getGameTimeDeltaPartialTick(true) * rotationSpeed, 360);
        var transform = new org.joml.Matrix4f().scale(-1, 1, -1)
            .translate(-sizeX / 2f + centerOffset, -sizeY / 2f, 0)
            .rotateX(-30 * Mth.DEG_TO_RAD).translate(-offsetX, 0, -offsetZ)
            .rotateY(((float) rotationY + 45) * Mth.DEG_TO_RAD).translate(offsetX, 0, offsetZ - 1);
        int extent = Math.max(1, previewSize / 2);
        StructurePreviewScene.of(level).submit(graphics, transform, posX - extent, posY - extent,
            posX + extent, posY + extent, scale, net.minecraft.world.phys.shapes.Shapes.empty(), scanEffect ? 0.75f : 0);
    }

    /**
     * 包围盒在该旋转下投影到预览窗口时所需的缩放系数。
     */
    public static float autoScale(
        Quaternionf rotation,
        AABB bounds,
        int windowWidth,
        int windowHeight,
        float padding
    ) {
        Matrix3f matrix = new Matrix3f().rotation(rotation);
        float sizeX = (float) bounds.getXsize();
        float sizeY = (float) bounds.getYsize();
        float sizeZ = (float) bounds.getZsize();
        float projectedWidth = Math.abs(matrix.m00()) * sizeX
            + Math.abs(matrix.m10()) * sizeY + Math.abs(matrix.m20()) * sizeZ;
        float projectedHeight = Math.abs(matrix.m01()) * sizeX
            + Math.abs(matrix.m11()) * sizeY + Math.abs(matrix.m21()) * sizeZ;
        return Math.min(
            (windowWidth - padding * 2) / Math.max(projectedWidth, 1.0e-4f),
            (windowHeight - padding * 2) / Math.max(projectedHeight, 1.0e-4f)
        );
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

    public static void renderGroundItemPreview(
        ItemStack stack, PoseStack pose, MultiBufferSource.BufferSource buffers, float scale, int seed
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || stack.isEmpty()) return;
        float radius = 0.15F * scale;
        VertexConsumer shadow = buffers.getBuffer(ITEM_SHADOW);
        PoseStack.Pose surface = pose.last();
        shadowVertex(shadow, surface, 0.5F - radius, 0.5F - radius, 0, 0);
        shadowVertex(shadow, surface, 0.5F - radius, 0.5F + radius, 0, 1);
        shadowVertex(shadow, surface, 0.5F + radius, 0.5F + radius, 1, 1);
        shadowVertex(shadow, surface, 0.5F + radius, 0.5F - radius, 1, 0);
        buffers.endBatch(ITEM_SHADOW);

        pose.pushPose();
        float bottom = FittedItemRenderer.getGroundItemBottom(stack, seed);
        pose.translate(0.5F, -bottom * scale, 0.5F);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, 0, -0.5F);
        BaseShowItemRenderer.renderItem(
            minecraft.level, stack, 0.5F, 0, 0.5F, minecraft.getItemRenderer(), pose,
            type -> buffers.getBuffer(FULL_BRIGHT_ITEM_TYPES.apply(type)), LightTexture.FULL_BRIGHT, getPartialTick(), seed
        );
        pose.popPose();
        buffers.endBatch();
    }

    private static void shadowVertex(VertexConsumer vertices, PoseStack.Pose surface, float x, float z, float u, float v) {
        vertices.addVertex(surface, x, 0.002F, z).setColor(255, 255, 255, 128).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(surface, 0, 1, 0);
    }

    private static RenderType fullBrightItemType(RenderType original) {
        if (original.format() == DefaultVertexFormat.NEW_ENTITY
            && original instanceof RenderType.CompositeRenderType composite
            && composite.state().textureState instanceof RenderStateShard.TextureStateShard textureState) {
            Optional<ResourceLocation> texture = textureState.texture;
            if (texture.isPresent()) {
                return RenderType.create(
                    "anvilcraft:full_bright_item", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
                    RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture.get(), false, false))
                        .setTransparencyState(composite.state().transparencyState)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .createCompositeState(false)
                );
            }
        }
        return original;
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
                // 在恢复 3D 光照前立即刷新批处理，否则透明物品会在后续（如 tooltip 触发）flush 时
                // 以 3D 光照着色而变暗，与原版 GuiGraphics.renderItem 的刷新时机保持一致
                Minecraft.getInstance().levelRenderer.renderBuffers.bufferSource().endBatch();
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

            bakedModel = ClientHooks.handleCameraTransforms(poseStack, bakedModel, displayContext, leftHand);
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
