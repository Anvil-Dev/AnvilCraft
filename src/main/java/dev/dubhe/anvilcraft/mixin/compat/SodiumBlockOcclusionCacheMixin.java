package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.client.event.NegativeShapeModelEventListener;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockRenderContext.class)
abstract class SodiumBlockOcclusionCacheMixin {
    @Shadow
    protected BlockAndTintGetter level;

    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @ModifyExpressionValue(method = "shouldDrawSide", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)"
            + "Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState anvilcraft$disguisedNeighbor(BlockState neighbor, Direction face) {
        return neighbor.getBlock() instanceof RuinsBlock
            ? RuinsBlock.disguisedState(this.level, this.pos.relative(face), neighbor) : neighbor;
    }

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void preserveNegativeShapeFace(Direction facing, CallbackInfoReturnable<Boolean> cir) {
        BlockState selfState = this.state;
        if (selfState.getBlock() instanceof INegativeShapeBlock<?> block) {
            BlockPos adjacent = this.pos.relative(facing);
            BlockState adjacentState = RuinsBlock.disguisedState(this.level, adjacent, this.level.getBlockState(adjacent));
            if (!block.getBlockType().isInstance(adjacentState.getBlock())) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(!NegativeShapeModelEventListener.shouldSkipFace(selfState, adjacentState, facing));
            }
        }
    }
}
