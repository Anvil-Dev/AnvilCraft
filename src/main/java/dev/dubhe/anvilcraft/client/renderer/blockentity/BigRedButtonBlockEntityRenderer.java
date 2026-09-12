package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BigRedButtonBlock;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;

public class BigRedButtonBlockEntityRenderer
    implements BlockEntityRenderer<BigRedButtonBlockEntity>, ModelSelectionRenderer<BigRedButtonBlockEntity> {
    private static final ModelResourceLocation CAP = ModelResourceLocation.standalone(AnvilCraft.of("block/big_red_button_cap"));

    public BigRedButtonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        BigRedButtonBlockEntity button, float partialTick, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight, int packedOverlay
    ) {
        this.collectSelectionModels(button, partialTick, poseStack, (model, pose) -> Minecraft.getInstance()
            .getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), bufferSource.getBuffer(RenderType.cutout()), null,
                Minecraft.getInstance().getModelManager().getModel(model), 1, 1, 1, packedLight, packedOverlay
            ));
    }

    @Override
    public void collectSelectionModels(BigRedButtonBlockEntity button, float partialTick, PoseStack pose, ModelConsumer consumer) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Direction facing = button.getBlockState().getValue(BigRedButtonBlock.FACING);
        int x = facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;
        int y = facing.getAxis().isVertical() ? 0 : (int) facing.toYRot() + 180;
        pose.mulPose(Axis.YP.rotationDegrees(-y));
        pose.mulPose(Axis.XP.rotationDegrees(-x));
        pose.translate(-0.5, -0.5 - button.getPressProgress(partialTick) * 2 / 16, -0.5);
        consumer.accept(CAP, pose);
        pose.popPose();
    }
}
