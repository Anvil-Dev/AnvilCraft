package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BatchCrafterRenderer implements BlockEntityRenderer<BatchCrafterBlockEntity> {
    private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
    private static final int ITEM_COUNT_FOR_5_BUNDLE = 48;
    private static final int ITEM_COUNT_FOR_4_BUNDLE = 32;
    private static final int ITEM_COUNT_FOR_3_BUNDLE = 16;
    private static final int ITEM_COUNT_FOR_2_BUNDLE = 1;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
    private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemRenderer itemRenderer;
    private final RandomSource random = RandomSource.create();

    public BatchCrafterRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderDispatcher = context.getBlockRenderDispatcher();
        itemRenderer = context.getItemRenderer();
    }

    private int getRenderAmount(ItemStack stack) {
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

    @Override
    public void render(
            BatchCrafterBlockEntity blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = blockEntity.getBlockState();
        BakedModel blockModel = blockRenderDispatcher.getBlockModel(state);
        Level level = blockEntity.getLevel();
        int packed = LightTexture.FULL_BLOCK;
        if (level != null) {
            int skyLight = level.getBrightness(LightLayer.SKY, blockEntity.getBlockPos());
            int blockLight = level.getBrightness(LightLayer.BLOCK, blockEntity.getBlockPos());
            packed = LightTexture.pack(blockLight, skyLight);
        }
        blockRenderDispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                state,
                blockModel,
                0,
                0,
                0,
                packed,
                OverlayTexture.NO_OVERLAY
        );
        ItemStack itemStack = blockEntity.getDisplayItemStack();
        if (itemStack == null || itemStack.isEmpty()) return;
        int seed = itemStack.isEmpty() ? 187 : Item.getId(itemStack.getItem()) + itemStack.getDamageValue();
        this.random.setSeed(seed);
        BakedModel bakedModel = this.itemRenderer.getModel(itemStack, level, null, blockEntity.getId());
        poseStack.pushPose();
        final boolean isGui3d = bakedModel.isGui3d();
        final int renderAmount = this.getRenderAmount(itemStack);
        float elevate = Mth.sin((blockEntity.getLevel().getGameTime() + partialTick) / 10.0F) * 0.1F + 0.1F;
        float transformedGroundScaleY = bakedModel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        poseStack.translate(0.5F, elevate + 0.6F * transformedGroundScaleY, 0.5F);
        float rotation = (blockEntity.getLevel().getGameTime() + partialTick) * 2f;
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
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
            if (i > 0) {
                if (isGui3d) {
                    float p = (this.random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE;
                    float q = (this.random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE;
                    float s = (this.random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE;
                    poseStack.translate(p, q, s);
                } else {
                    float p = (this.random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE * 0.5F;
                    float q = (this.random.nextFloat() * 2.0F - 1.0F) * ITEM_BUNDLE_OFFSET_SCALE * 0.5F;
                    poseStack.translate(p, q, 0.0F);
                }
            }

            this.itemRenderer.render(
                    itemStack,
                    ItemDisplayContext.GROUND,
                    false,
                    poseStack,
                    buffer,
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
}
