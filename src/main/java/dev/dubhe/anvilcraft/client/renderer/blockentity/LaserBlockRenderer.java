package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;

import dev.dubhe.anvilcraft.client.renderer.laser.LaserCompiler;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.renderer.laser.LaserState;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

import com.mojang.blaze3d.vertex.PoseStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserBlockRenderer implements BlockEntityRenderer<BaseLaserBlockEntity> {

    @SuppressWarnings("unused")
    public LaserBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        BaseLaserBlockEntity baseLaserBlockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (RenderState.isEnhancedRenderingAvailable()) return;
        poseStack.pushPose();
        LaserState laserState = LaserState.create(baseLaserBlockEntity, poseStack);
        if (laserState != null) {
            LaserCompiler.compile(
                laserState,
                buffer::getBuffer
            );
        }
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.translucent());
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BaseLaserBlockEntity blockEntity) {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        return AABB.ofSize(
            blockEntity.getBlockPos().getCenter(),
            renderDistance * 2,
            renderDistance * 2,
            renderDistance * 2
        );
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(BaseLaserBlockEntity blockEntity) {
        return true;
    }
}
