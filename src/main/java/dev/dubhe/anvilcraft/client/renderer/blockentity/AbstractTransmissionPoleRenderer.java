package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.AbstractTransmissionPoleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractTransmissionPoleRenderer<T extends AbstractTransmissionPoleBlockEntity>
        implements BlockEntityRenderer<T> {

    @Override
    public void render(
            AbstractTransmissionPoleBlockEntity blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (!isHeadBlock(blockEntity.getBlockState())) return;

    }

    protected abstract boolean isHeadBlock(BlockState blockState);
}
