package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import dev.dubhe.anvilcraft.client.event.NegativeShapeModelEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockOcclusionCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockOcclusionCache.class)
abstract class EmbBlockOcclusionCacheMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void preserveNegativeShapeFace(
        BlockState selfState,
        BlockGetter view,
        BlockPos pos,
        Direction facing,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (selfState.getBlock() instanceof INegativeShapeBlock<?> block) {
            BlockState adjacentState = view.getBlockState(pos.relative(facing));
            if (!block.getBlockType().isInstance(adjacentState.getBlock())) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(!NegativeShapeModelEventListener.shouldSkipFace(selfState, adjacentState, facing));
            }
        }
    }
}
