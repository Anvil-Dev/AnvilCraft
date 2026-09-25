package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BigRedButtonRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public class BigRedButtonBlockEntityRenderer
    implements BlockEntityRenderer<BigRedButtonBlockEntity, BigRedButtonRenderState>, ModelSelectionRenderer<BigRedButtonBlockEntity> {
    public static final StandaloneModelKey<BlockStateModel> CAP = new StandaloneModelKey<>(() -> "AnvilCraft: BigRedButton");

    public BigRedButtonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BigRedButtonRenderState createRenderState() {
        return new BigRedButtonRenderState();
    }

    @Override
    public void extractRenderState(
        BigRedButtonBlockEntity blockEntity, BigRedButtonRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        extractAnimation(blockEntity, state, partialTick);
        state.model = FeatureRendererSupport.initialize(CAP, blockEntity);
    }

    private static void extractAnimation(BigRedButtonBlockEntity blockEntity, BigRedButtonRenderState state, float partialTick) {
        state.progress = blockEntity.getPressProgress(partialTick);
        state.facing = blockEntity.getBlockState().getValue(BigRedButtonBlock.FACING);
    }

    @Override
    public void collectSelectionModels(BigRedButtonBlockEntity blockEntity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        BigRedButtonRenderState state = new BigRedButtonRenderState();
        extractAnimation(blockEntity, state, partialTick);
        collectModels(state, pose, consumer);
    }

    @Override
    public void submit(BigRedButtonRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        collectModels(state, pose, (model, modelPose) ->
            state.model.submit(modelPose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0));
    }

    private static void collectModels(BigRedButtonRenderState state, PoseStack pose, ModelConsumer consumer) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Direction facing = state.facing;
        int x = facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;
        int y = facing.getAxis().isVertical() ? 0 : (int) facing.toYRot() + 180;
        pose.mulPose(Axis.YP.rotationDegrees(-y));
        pose.mulPose(Axis.XP.rotationDegrees(-x));
        pose.translate(-0.5, -0.5 - state.progress * 2 / 16, -0.5);
        consumer.accept(CAP, pose);
        pose.popPose();
    }
}
