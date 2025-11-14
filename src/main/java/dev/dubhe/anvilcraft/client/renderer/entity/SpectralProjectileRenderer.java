package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.entity.SpectralProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.Iterator;

public class SpectralProjectileRenderer<T extends SpectralProjectileEntity> extends ArrowRenderer<T> {

    public static final ResourceLocation ARROW_LOCATION = ResourceLocation.withDefaultNamespace(
        "textures/entity/projectiles/arrow.png"
    );
    private final ItemRenderer itemRenderer;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
    // 用不上的随机：private final RandomSource random = RandomSource.create();

    public SpectralProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(T t) {
        return ARROW_LOCATION;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(
        T entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
    ) {
        // 用不上的level：Level level = entity.level();
        ItemStack itemStack = entity.getAsItemStack();
        if (itemStack.is(ItemTags.ARROWS)) {
            // 由于不再能装载箭矢了，所以不半透明也无所谓了（）
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        BakedModel bakedModel = this.itemRenderer.getItemModelShaper().getItemModel(itemStack);
        // 常规的方法是this.itemRenderer.getModel(itemStack, level, null, 0);，只不过我觉得三叉戟和望远镜还是用平面图合理点
        poseStack.pushPose();
        final boolean isGui3d = bakedModel.isGui3d();
        final int renderAmount = 1;
        float transformedGroundScaleY = bakedModel
            .getTransforms()
            .getTransform(ItemDisplayContext.GROUND)
            .scale
            .y();
        poseStack.translate(0F, 0.5F * transformedGroundScaleY - 0.1f, 0F);
        Vec2 rotationVector = entity.getRotationVector();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationVector.y - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationVector.x - 45));
        float groundScaleX = bakedModel.getTransforms().ground.scale.x();
        float groundScaleY = bakedModel.getTransforms().ground.scale.y();
        float groundScaleZ = bakedModel.getTransforms().ground.scale.z();

        if (!isGui3d) {
            float ox = -FLAT_ITEM_BUNDLE_OFFSET_X * (float) (renderAmount - 1) * 0.5F * groundScaleX;
            float oy = -FLAT_ITEM_BUNDLE_OFFSET_Y * (float) (renderAmount - 1) * 0.5F * groundScaleY;
            float oz = -FLAT_ITEM_BUNDLE_OFFSET_Z * (float) (renderAmount - 1) * 0.5F * groundScaleZ;
            poseStack.translate(ox, oy, oz);
        }
        for (int i = 0; i < renderAmount; ++i) {
            poseStack.pushPose();

            this.renderTranslucentItem(
                128,
                itemStack,
                ItemDisplayContext.GROUND,
                false,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                bakedModel
            );
            poseStack.popPose();
            if (!isGui3d) {
                poseStack.translate(
                    FLAT_ITEM_BUNDLE_OFFSET_X * groundScaleX,
                    FLAT_ITEM_BUNDLE_OFFSET_Y * groundScaleY,
                    FLAT_ITEM_BUNDLE_OFFSET_Z * groundScaleZ
                );
            }
        }
        poseStack.popPose();
    }

    public void renderTranslucentItem(
        int alpha,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        boolean leftHand, PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        BakedModel bakedModel
    ) {
        if (!itemStack.isEmpty()) {
            poseStack.pushPose();
            boolean flag = displayContext == ItemDisplayContext.GUI
                || displayContext == ItemDisplayContext.GROUND
                || displayContext == ItemDisplayContext.FIXED;

            bakedModel = ClientHooks.handleCameraTransforms(poseStack, bakedModel, displayContext, leftHand);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            if (!bakedModel.isCustomRenderer() && flag) {
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

                for (BakedModel model : bakedModel.getRenderPasses(itemStack, flag1)) {
                    VertexConsumer vertexconsumer;
                    for (
                        Iterator<RenderType> var13 = model.getRenderTypes(itemStack, flag1).iterator();
                        var13.hasNext();
                        this.itemRenderer.renderModelLists(model, itemStack, combinedLight, combinedOverlay, poseStack, vertexconsumer)
                    ) {
                        RenderType rendertype = var13.next();
                        VertexConsumer originalConsumer = bufferSource.getBuffer(rendertype);
                        vertexconsumer = new TranslucentVertexConsumer(originalConsumer, alpha);
                    }
                }
            } else {
                IClientItemExtensions
                    .of(itemStack)
                    .getCustomRenderer()
                    .renderByItem(itemStack, displayContext, poseStack, bufferSource, combinedLight, combinedOverlay);
            }

            poseStack.popPose();
        }

    }

    /**
     * 透明顶点消费者
     *
     * @param delegate 原始顶点消费者（委托对象）
     * @param alpha    目标半透明度（0-255，50% 对应 128）
     */
    public record TranslucentVertexConsumer(VertexConsumer delegate, int alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float v, float v1, float v2) {
            return delegate.addVertex(v, v1, v2);
        }

        @Override
        public VertexConsumer setColor(int i, int i1, int i2, int i3) {
            return delegate.setColor(i, i1, i2, this.alpha);
        }

        @Override
        public VertexConsumer setUv(float v, float v1) {
            return delegate.setUv(v, v1);
        }

        @Override
        public VertexConsumer setUv1(int i, int i1) {
            return delegate.setUv1(i, i1);
        }

        @Override
        public VertexConsumer setUv2(int i, int i1) {
            return delegate.setUv2(i, i1);
        }

        @Override
        public VertexConsumer setNormal(float v, float v1, float v2) {
            return delegate.setNormal(v, v1, v2);
        }
    }
}


