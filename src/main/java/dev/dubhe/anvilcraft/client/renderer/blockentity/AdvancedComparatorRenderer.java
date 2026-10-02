package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.AdvancedComparatorRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public class AdvancedComparatorRenderer
    implements BlockEntityRenderer<AdvancedComparatorBlockEntity, AdvancedComparatorRenderState>,
    ModelSelectionRenderer<AdvancedComparatorBlockEntity> {
    public static final StandaloneModelKey<BlockStateModel> INDICATOR = new StandaloneModelKey<>(
        () -> "AnvilCraft: Advanced Comparator Indicator Model"
    );

    public AdvancedComparatorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public AdvancedComparatorRenderState createRenderState() {
        return new AdvancedComparatorRenderState();
    }

    @Override
    public void extractRenderState(
        AdvancedComparatorBlockEntity be,
        AdvancedComparatorRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setSignal(be.getBlockState().getValue(AdvancedComparatorBlock.POWER));
        state.setIndicator(FeatureRendererSupport.initialize(AdvancedComparatorRenderer.INDICATOR, be));
    }

    @Override
    public void submit(AdvancedComparatorRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        applyIndicatorPose(pose, state.getSignal());
        state.getIndicator().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public void collectSelectionModels(AdvancedComparatorBlockEntity be, float partialTick, PoseStack pose, ModelConsumer consumer) {
        pose.pushPose();
        applyIndicatorPose(pose, be.getBlockState().getValue(AdvancedComparatorBlock.POWER));
        consumer.accept(INDICATOR, pose);
        pose.popPose();
    }

    private static void applyIndicatorPose(PoseStack pose, int signal) {
        pose.translate(0, signal / 3F * .0625F, 0);
    }
}
