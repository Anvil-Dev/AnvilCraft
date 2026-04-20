package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class WipBlockEntityRenderer implements BlockEntityRenderer<WipBlockEntity> {

    public WipBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WipBlockEntity wipBlockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int i1) {

    }
}
