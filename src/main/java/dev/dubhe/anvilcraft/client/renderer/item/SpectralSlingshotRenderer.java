package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.SpectralSlingshotItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public class SpectralSlingshotRenderer extends BlockEntityWithoutLevelRenderer {
    private static SpectralSlingshotRenderer instance;

    public static SpectralSlingshotRenderer getInstance() {
        if (instance == null) {
            instance = new SpectralSlingshotRenderer(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
            );
        }
        return instance;
    }

    public SpectralSlingshotRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        if (stack.is(ModItems.SPECTRAL_SLINGSHOT)) {
            BakedModel normalModel = itemRenderer.getItemModelShaper().getItemModel(stack);
            renderItemAtCurrentPoseStack(itemRenderer,
                stack,
                displayContext,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                normalModel
            );
            ChargedProjectiles cp = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (cp != null && !cp.isEmpty()) {
                ItemStack ammo = cp.getItems().getFirst();
                poseStack.pushPose();
                poseStack.translate(0f, 0.7f, 0.50F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(- 45));
                // poseStack.pushPose();
                BakedModel bakedModel = itemRenderer.getItemModelShaper().getItemModel(ammo);
                renderItemAtCurrentPoseStack(
                    itemRenderer,
                    ammo,
                    displayContext,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay,
                    bakedModel
                );
                // poseStack.popPose();
                poseStack.popPose();
            }
        }
    }

    public static void renderItemAtCurrentPoseStack(
        ItemRenderer itemRenderer,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        BakedModel model
    ) {
        boolean flag1;
        label78: {
            if (displayContext != ItemDisplayContext.GUI && !displayContext.firstPerson()) {
                Item var12 = itemStack.getItem();
                if (var12 instanceof BlockItem blockitem) {
                    Block block = blockitem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                    break label78;
                }
            }
            flag1 = true;
        }
        for (BakedModel model1 : model.getRenderPasses(itemStack, flag1)) {
            VertexConsumer vertexconsumer;
            for (Iterator<RenderType> var13 = model1.getRenderTypes(itemStack, flag1).iterator();
                 var13.hasNext();
                 itemRenderer.renderModelLists(
                     model1,
                     itemStack,
                     combinedLight,
                     combinedOverlay,
                     poseStack,
                     vertexconsumer
                 )
            ) {
                RenderType rendertype = var13.next();
                vertexconsumer = ItemRenderer.getFoilBuffer(bufferSource, rendertype, true, itemStack.hasFoil());
            }
        }
    }

    public static class SpectralSlingshotExtensions extends CustomRenderItemClientExtension {
        protected SpectralSlingshotExtensions(BlockEntityWithoutLevelRenderer renderer) {
            super(renderer);
        }

        @Nullable
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
            if (itemStack.is(ModItems.SPECTRAL_SLINGSHOT) && SpectralSlingshotItem.isCharged(itemStack)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            return super.getArmPose(entityLiving, hand, itemStack);
        }
    }
}
