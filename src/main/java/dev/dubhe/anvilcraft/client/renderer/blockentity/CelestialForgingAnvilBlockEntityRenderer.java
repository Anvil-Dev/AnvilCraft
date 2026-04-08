package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class CelestialForgingAnvilBlockEntityRenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity> {
    private static final ModelResourceLocation RING1 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1"));
    private static final ModelResourceLocation RING2 = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2"));

    @SuppressWarnings("deprecation")
    @Override
    public void render(CelestialForgingAnvilBlockEntity blockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1) {
        ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        poseStack.pushPose();
        final VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.cutout());
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(blockEntity.getRotation()));
        if (blockEntity.getBlockState().getValue(CelestialForgingAnvilBlock.HALF) == Cube3x3PartHalf.TOP_CENTER) {
            poseStack.scale(4, 4, 4);
            modelRenderer.renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(RING2),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                i1
            );

            poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getRotation()));
            modelRenderer.renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(RING1),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                i1
            );
        }
        poseStack.popPose();
    }
}
