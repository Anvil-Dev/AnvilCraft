package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CelestialForgingAnvilBlockEntityRenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity> {
    private static final ModelResourceLocation RING1 =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_1"));
    private static final ModelResourceLocation RING2 =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_2"));
    private static final ModelResourceLocation RING3 =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_forging_anvil_ring_3"));

    public CelestialForgingAnvilBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(
        CelestialForgingAnvilBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource multiBufferSource,
        int packedLight,
        int packedOverlay
    ) {
        final ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        float rotation = blockEntity.getRotation() + (blockEntity.getRotation() - blockEntity.getPreRotation()) * partialTick;
        poseStack.pushPose();
        final VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.cutout());
        if (blockEntity.isAmplify()) {
            poseStack.translate(0.5, 4.5, 0.5);
        } else {
            poseStack.translate(0.5, 3.5, 0.5);
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation));
        if (blockEntity.isAmplify()) {
            poseStack.scale(4, 4, 4);
            modelRenderer.renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(RING3),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                packedOverlay
            );

            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            modelRenderer.renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(RING2),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                packedOverlay
            );
        } else {
            poseStack.scale(4, 4, 4);
            modelRenderer.renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(RING2),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                packedOverlay
            );

            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            modelRenderer.renderModel(
                poseStack.last(),
                vertexConsumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(RING1),
                0, 0, 0,
                LightTexture.FULL_BLOCK,
                packedOverlay
            );
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(
                blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())
            ).inflate(1, 0, 1);
            return aabb.setMaxY(aabb.maxY + 5);
        }
        AABB aabb = new AABB(
            blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())
        ).inflate(3, 0, 3);
        return aabb.setMaxY(aabb.maxY + 7);
    }
}
