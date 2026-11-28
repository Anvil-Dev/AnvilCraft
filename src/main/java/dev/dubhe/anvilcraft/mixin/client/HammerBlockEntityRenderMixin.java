package dev.dubhe.anvilcraft.mixin.client;

import dev.dubhe.anvilcraft.client.building.BuildingRodPlacementAnimation;
import dev.dubhe.anvilcraft.client.support.HammerPreviewState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 区块中的实体和全局实体共用此提取入口，隐藏后也跳过旧朝向的动画更新。 */
@NullMarked
@Mixin(BlockEntityRenderDispatcher.class)
abstract class HammerBlockEntityRenderMixin {
    @Inject(method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;F"
        + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/culling/Frustum;)"
        + "Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$hideOriginalEntity(
        BlockEntity entity, float partialTick, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay,
        @Nullable Frustum frustum, CallbackInfoReturnable<BlockEntityRenderState> callback
    ) {
        if (BuildingRodPlacementAnimation.hidesBlock(entity.getBlockPos()) || HammerPreviewState.hidesBlock(entity.getBlockPos())) {
            callback.setReturnValue(null);
        }
    }
}
