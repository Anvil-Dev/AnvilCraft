package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import dev.dubhe.anvilcraft.client.event.NegativeShapeModelEventListener;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void negativeShapeFaceSkip(
        BlockState adjacentState,
        Direction face,
        CallbackInfoReturnable<Boolean> cir
    ) {
        BlockState state = this.asState();
        if (state.getBlock() instanceof INegativeShapeBlock<?> block
            && block.getBlockType().isInstance(adjacentState.getBlock())) {
            cir.setReturnValue(
                NegativeShapeModelEventListener.shouldSkipFace(state, adjacentState, face)
            );
        }
    }
}
