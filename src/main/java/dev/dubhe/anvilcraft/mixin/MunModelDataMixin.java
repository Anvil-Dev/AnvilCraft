package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelDataManager.class)
abstract class MunModelDataMixin {
    @Inject(method = "requestRefresh", at = @At("RETURN"))
    private void anvilcraft$invalidateModelShadow(BlockEntity entity, CallbackInfo ci) {
        MunSurfaceRenderer.onModelDataChanged(entity);
    }
}
