package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PlasmaJetsRenderer implements BlockEntityRenderer<PlasmaJetsBlockEntity> {
    public PlasmaJetsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        PlasmaJetsBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource bufferSource,
        int packedLight, int packedOverlay
    ) {

    }
}
