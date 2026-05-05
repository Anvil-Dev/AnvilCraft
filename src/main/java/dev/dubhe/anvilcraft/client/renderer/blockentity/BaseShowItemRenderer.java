package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public abstract class BaseShowItemRenderer<B extends BlockEntity> implements BlockEntityRenderer<B> {
    private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
    private static final int ITEM_COUNT_FOR_5_BUNDLE = 48;
    private static final int ITEM_COUNT_FOR_4_BUNDLE = 32;
    private static final int ITEM_COUNT_FOR_3_BUNDLE = 16;
    private static final int ITEM_COUNT_FOR_2_BUNDLE = 1;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
    private final ItemRenderer itemRenderer;

    public BaseShowItemRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    private static int getRenderAmount(ItemStack stack) {
        int i = 1;
        if (stack.getCount() > ITEM_COUNT_FOR_5_BUNDLE) {
            i = 5;
        } else if (stack.getCount() > ITEM_COUNT_FOR_4_BUNDLE) {
            i = 4;
        } else if (stack.getCount() > ITEM_COUNT_FOR_3_BUNDLE) {
            i = 3;
        } else if (stack.getCount() > ITEM_COUNT_FOR_2_BUNDLE) {
            i = 2;
        }
        return i;
    }

    @Nullable
    protected abstract ItemStack getDisplayItemStack(B blockEntity);

    protected abstract int getSeed(B blockEntity);

    @Override
    public void render(
        B be,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        BaseShowItemRenderer.renderItem(
            be.getLevel(),
            this.getDisplayItemStack(be),
            0.5F,
            0.5F,
            0.5F,
            this.itemRenderer,
            poseStack,
            buffer,
            packedLight,
            partialTick,
            this.getSeed(be)
        );
    }

    public static void renderItem(
        Level level,
        @Nullable ItemStack stack,
        float x,
        float y,
        float z,
        ItemRenderer renderer,
        PoseStack pose,
        MultiBufferSource buffer,
        int packedLight,
        float partialTick,
        int seed
    ) {
        if (stack == null || stack.isEmpty()) return;
        final RandomSource random = RandomSource.create(Item.getId(stack.getItem()) + stack.getDamageValue());
        BakedModel bakedModel = renderer.getModel(stack, level, null, seed);
        pose.pushPose();
        final boolean isGui3d = bakedModel.isGui3d();
        final int renderAmount = BaseShowItemRenderer.getRenderAmount(stack);
        @SuppressWarnings("deprecation")
        float transformedGroundScaleY = bakedModel
            .getTransforms()
            .getTransform(ItemDisplayContext.GROUND)
            .scale
            .y();
        pose.translate(x, y * transformedGroundScaleY + 0.15f, z);
        float rotation = (level.getGameTime() + partialTick) * 2f;
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        @SuppressWarnings("deprecation")
        float groundScaleX = bakedModel.getTransforms().ground.scale.x();
        @SuppressWarnings("deprecation")
        float groundScaleY = bakedModel.getTransforms().ground.scale.y();
        @SuppressWarnings("deprecation")
        float groundScaleZ = bakedModel.getTransforms().ground.scale.z();

        if (!isGui3d) {
            float ox = -FLAT_ITEM_BUNDLE_OFFSET_X * (float) (renderAmount - 1) * x * groundScaleX;
            float oy = -FLAT_ITEM_BUNDLE_OFFSET_Y * (float) (renderAmount - 1) * y * groundScaleY;
            float oz = -FLAT_ITEM_BUNDLE_OFFSET_Z * (float) (renderAmount - 1) * z * groundScaleZ;
            pose.translate(ox, oy, oz);
        }
        for (int i = 0; i < renderAmount; ++i) {
            pose.pushPose();
            if (i > 0) {
                if (isGui3d) {
                    float p = (random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE;
                    float q = (random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE;
                    float s = (random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE;
                    pose.translate(p, q, s);
                } else {
                    float p = (random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE * x;
                    float q = (random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE * y;
                    pose.translate(p, q, 0.0F);
                }
            }

            renderer.render(
                stack,
                ItemDisplayContext.GROUND,
                false,
                pose,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                bakedModel
            );
            pose.popPose();
            if (!isGui3d) {
                pose.translate(
                    FLAT_ITEM_BUNDLE_OFFSET_X * groundScaleX,
                    FLAT_ITEM_BUNDLE_OFFSET_Y * groundScaleY,
                    FLAT_ITEM_BUNDLE_OFFSET_Z * groundScaleZ
                );
            }
        }
        pose.popPose();
    }
}
