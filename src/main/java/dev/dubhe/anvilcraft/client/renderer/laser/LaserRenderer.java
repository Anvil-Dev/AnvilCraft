package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.rendering.CacheableBlockEntityRenderer;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;

public class LaserRenderer implements CacheableBlockEntityRenderer<BaseLaserBlockEntity> {

    @Override
    public void render(
        BaseLaserBlockEntity blockEntity,
        MultiBufferSource.BufferSource buffer,
        PoseStack poseStack
    ) {
        LaserState laserState = LaserState.create(blockEntity, poseStack);
        if (laserState != null) {
            LaserCompiler.compile(
                laserState,
                buffer::getBuffer
            );
        }
    }
}
