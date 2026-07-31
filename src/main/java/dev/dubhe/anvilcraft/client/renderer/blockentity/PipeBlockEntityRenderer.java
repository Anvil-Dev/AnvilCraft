package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.fluid.PipeCheckValveBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeCheckValveBlockEntity> {

    private final GlassPipeFluidBERenderer glassPipeFluidRenderer;
    private final PipeCheckValveBERenderer pipeCheckValveRenderer;

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.glassPipeFluidRenderer = new GlassPipeFluidBERenderer(context);
        this.pipeCheckValveRenderer = new PipeCheckValveBERenderer(context);
    }

    @Override
    public void render(
        PipeCheckValveBlockEntity be,
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
