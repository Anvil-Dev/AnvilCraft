package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.building.BuildingRodPlacementAnimation;
import dev.dubhe.anvilcraft.client.support.HammerPreviewState;
import dev.dubhe.anvilcraft.client.support.MagnetAnvilAnimation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sodium 独立编译区块模型，需要在它的模型入口应用预览与动画的隐藏条件。 */
@Mixin(value = BlockRenderer.class, remap = false)
abstract class SodiumHiddenBlockMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$hideBlockModel(
        BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci
    ) {
        if (BuildingRodPlacementAnimation.hidesBlock(pos) || HammerPreviewState.hidesBlock(pos)
            || MagnetAnvilAnimation.hidesBlock(pos, state)) {
            ci.cancel();
        }
    }
}
