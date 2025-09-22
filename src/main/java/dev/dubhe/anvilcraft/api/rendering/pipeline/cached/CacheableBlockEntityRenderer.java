package dev.dubhe.anvilcraft.api.rendering.pipeline.cached;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface CacheableBlockEntityRenderer<T extends BlockEntity> {
    void render(
        T cacheableBlockEntity,
        MultiBufferSource.BufferSource buffer,
        PoseStack poseStack
    );
}
