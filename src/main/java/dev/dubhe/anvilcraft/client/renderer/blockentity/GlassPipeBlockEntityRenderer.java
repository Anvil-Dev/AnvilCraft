package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class GlassPipeBlockEntityRenderer
    implements BlockEntityRenderer<GlassPipeBlockEntity>, ModelSelectionRenderer<GlassPipeBlockEntity> {
    private final GlassPipeFluidBERenderer glassPipeFluidRenderer;
    private final PipeCheckValveBERenderer<GlassPipeBlockEntity> pipeCheckValveRenderer;

    public GlassPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.glassPipeFluidRenderer = new GlassPipeFluidBERenderer(context);
        this.pipeCheckValveRenderer = new PipeCheckValveBERenderer<>(context);
    }

    @Override
    public void collectSelectionModels(GlassPipeBlockEntity entity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        this.pipeCheckValveRenderer.collectSelectionModels(entity, partialTick, pose, consumer);
    }

    @Override
    public void render(
        GlassPipeBlockEntity be,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        this.glassPipeFluidRenderer.render(be, partialTick, poseStack, buffer, packedLight, packedOverlay);
        this.pipeCheckValveRenderer.render(be, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }
}
