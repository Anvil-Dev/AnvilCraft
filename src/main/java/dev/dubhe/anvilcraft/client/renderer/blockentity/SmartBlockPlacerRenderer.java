package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_arm")
    );

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        SmartBlockPlacerBlockEntity entity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof SmartBlockPlacerBlock)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        
        poseStack.pushPose();
        
        // 根据朝向旋转模型
        float rotation = switch (facing) {
            case NORTH -> 0f;
            case WEST -> 90f;
            case SOUTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
        
        // 平移到方块中心并应用旋转
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, -0.5, -0.5);
        
        // 渲染模型
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                vertexConsumer,
                state,
                Minecraft.getInstance().getModelManager().getModel(MODEL),
                0,
                0,
                0,
                packedLight,
                packedOverlay
            );
        
        poseStack.popPose();
    }
}
