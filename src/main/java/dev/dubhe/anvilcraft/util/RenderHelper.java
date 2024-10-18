package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderHelper {
    private static final Vector3f L1 = new Vector3f(0.4F, 0.0F, 1.0F).normalize();
    private static final Vector3f L2 = new Vector3f(-0.4F, 1.0F, -0.2F).normalize();

    public static final BlockRenderFunction SINGLE_BLOCK = (block, poseStack, buffers) -> Minecraft.getInstance()
        .getBlockRenderer()
        .renderSingleBlock(block, poseStack, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);

    private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident"));
    private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass"));

    public static void renderBlock(
        GuiGraphics guiGraphics, BlockState block, float x, float y, float z, float scale, BlockRenderFunction fn) {
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(-scale, -scale, -scale);
        poseStack.translate(-0.5f, -0.5f, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30F));
        poseStack.translate(0.5F, 0, -0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45f));
        poseStack.translate(-0.5F, 0, 0.5F);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        poseStack.translate(0, 0, -1);

        FluidState fluidState = block.getFluidState();
        if (fluidState.isEmpty()) {
            MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

            RenderSystem.setupGui3DDiffuseLighting(L1, L2);
            fn.renderBlock(block, poseStack, buffers);

            buffers.endBatch();
        }

        poseStack.popPose();
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
        BakedModel p_model,
        float alpha
    ) {
        if (!itemStack.isEmpty()) {
            poseStack.pushPose();
            boolean flag = displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.GROUND || displayContext == ItemDisplayContext.FIXED;
            if (flag) {
                if (itemStack.is(Items.TRIDENT)) {
                    p_model = itemRenderer.getItemModelShaper().getModelManager().getModel(TRIDENT_MODEL);
                } else if (itemStack.is(Items.SPYGLASS)) {
                    p_model = itemRenderer.getItemModelShaper().getModelManager().getModel(SPYGLASS_MODEL);
                }
            }

            p_model = net.neoforged.neoforge.client.ClientHooks.handleCameraTransforms(poseStack, p_model, displayContext, leftHand);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            if (!p_model.isCustomRenderer() && (!itemStack.is(Items.TRIDENT) || flag)) {
                boolean flag1;
                if (displayContext != ItemDisplayContext.GUI && !displayContext.firstPerson() && itemStack.getItem() instanceof BlockItem blockitem) {
                    Block block = blockitem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                } else {
                    flag1 = true;
                }

                for (BakedModel model : p_model.getRenderPasses(itemStack, flag1)) {
                    for (RenderType rendertype : model.getRenderTypes(itemStack, flag1)) {
                        VertexConsumer vertexconsumer;
                        if (hasAnimatedTexture(itemStack) && itemStack.hasFoil()) {
                            PoseStack.Pose posestack$pose = poseStack.last().copy();
                            if (displayContext == ItemDisplayContext.GUI) {
                                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
                            } else if (displayContext.firstPerson()) {
                                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
                            }

                            vertexconsumer = getCompassFoilBuffer(bufferSource, rendertype, posestack$pose);
                        } else {
                            if (flag1) {
                                vertexconsumer = getFoilBufferDirect(bufferSource, rendertype, true, itemStack.hasFoil());
                            } else {
                                vertexconsumer = getFoilBuffer(bufferSource, rendertype, true, itemStack.hasFoil());
                            }
                        }

                        renderModelListsWithTransparency(itemRenderer, model, itemStack, combinedLight, combinedOverlay, poseStack, vertexconsumer, alpha);
                    }
                }
            } else {
                net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemStack).getCustomRenderer().renderByItem(itemStack, displayContext, poseStack, bufferSource, combinedLight, combinedOverlay);
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
            new SheetedDecalTextureGenerator(bufferSource.getBuffer(RenderType.glint()), pose, 0.0078125F), bufferSource.getBuffer(useTranslucentIfPossible(renderType))
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean glint) {
        if (glint) {
            return Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
                ? VertexMultiConsumer.create(bufferSource.getBuffer(RenderType.glintTranslucent()), bufferSource.getBuffer(useTranslucentIfPossible(renderType)))
                : VertexMultiConsumer.create(bufferSource.getBuffer(isItem ? RenderType.glint() : RenderType.entityGlint()), bufferSource.getBuffer(useTranslucentIfPossible(renderType)));
        } else {
            return bufferSource.getBuffer(renderType);
        }
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource bufferSource, RenderType renderType, boolean noEntity, boolean withGlint) {
        return withGlint
            ? VertexMultiConsumer.create(bufferSource.getBuffer(noEntity ? RenderType.glint() : RenderType.entityGlintDirect()), bufferSource.getBuffer(useTranslucentIfPossible(renderType)))
            : bufferSource.getBuffer(useTranslucentIfPossible(renderType));
    }

    public static void renderModelListsWithTransparency(ItemRenderer itemRenderer, BakedModel model, ItemStack stack, int combinedLight, int combinedOverlay, PoseStack poseStack, VertexConsumer buffer, float alpha) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.values()) {
            randomsource.setSeed(42L);
            renderQuadListWithTransparency(itemRenderer, poseStack, buffer, model.getQuads(null, direction, randomsource), stack, combinedLight, combinedOverlay, alpha);
        }

        randomsource.setSeed(42L);
        renderQuadListWithTransparency(itemRenderer, poseStack, buffer, model.getQuads(null, null, randomsource), stack, combinedLight, combinedOverlay, alpha);
    }

    private static void renderQuadListWithTransparency(ItemRenderer itemRenderer, PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads, ItemStack itemStack, int combinedLight, int combinedOverlay, float alpha) {
        boolean flag = !itemStack.isEmpty();
        PoseStack.Pose posestack$pose = poseStack.last();

        for (BakedQuad bakedquad : quads) {
            int i = -1;
            if (flag && bakedquad.isTinted()) {
                i = itemRenderer.itemColors.getColor(itemStack, bakedquad.getTintIndex());
            }

            float f1 = (float) FastColor.ARGB32.red(i) / 255.0F;
            float f2 = (float) FastColor.ARGB32.green(i) / 255.0F;
            float f3 = (float) FastColor.ARGB32.blue(i) / 255.0F;
            buffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, alpha, combinedLight, combinedOverlay, true); // Neo: pass readExistingColor=true
        }
    }

    private static boolean hasAnimatedTexture(ItemStack stack) {
        return stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK);
    }

    @FunctionalInterface
    public interface BlockRenderFunction {
        void renderBlock(BlockState block, PoseStack poseStack, MultiBufferSource.BufferSource buffers);
    }
}
