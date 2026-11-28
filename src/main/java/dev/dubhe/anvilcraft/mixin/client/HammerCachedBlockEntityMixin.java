package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.dubhe.anvilcraft.client.building.BuildingRodPlacementAnimation;
import dev.dubhe.anvilcraft.client.support.HammerPreviewEntity;
import dev.dubhe.anvilcraft.client.support.HammerPreviewState;
import net.minecraft.client.Camera;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 缓存重建时排除预览目标，虚影仍可单独提取该实体的渲染数据。 */
@NullMarked
@Mixin(value = CachedBlockEntityRenderDispatcher.class, remap = false)
abstract class HammerCachedBlockEntityMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$hideCachedEntity(
        BlockEntity entity, float partialTick, Camera camera, CallbackInfoReturnable<CachedBlockEntityRenderState> callback
    ) {
        if (!HammerPreviewEntity.isExtracting() && (BuildingRodPlacementAnimation.hidesBlock(entity.getBlockPos())
            || HammerPreviewState.hidesBlock(entity.getBlockPos()))) {
            callback.setReturnValue(null);
        }
    }
}
