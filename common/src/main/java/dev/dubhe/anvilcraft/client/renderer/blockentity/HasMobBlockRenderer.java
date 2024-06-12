package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

public class HasMobBlockRenderer implements BlockEntityRenderer<HasMobBlockEntity> {
    @SuppressWarnings("unused")
    public HasMobBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            @NotNull HasMobBlockEntity blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        Entity entity = blockEntity.getOrCreateDisplayEntity(blockEntity.getLevel());
        if (entity == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);
        float size = 0.73125f;
        float max = Math.max(entity.getBbWidth(), entity.getBbHeight());
        if ((double) max > 1.0) {
            size /= max;
        }
        poseStack.translate(0.0f, 0.14f, 0.0f);
        poseStack.scale(size, size, size);
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.render(entity, 0, 0, 0, 0, 0, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
