package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.EmberBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
abstract class BlockMixin {

    @Inject(method = "shouldRenderFace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canOcclude()Z"),
            cancellable = true)
    private static void emberMetalBlockFaceSkip(
            BlockState state,
            BlockGetter level,
            BlockPos offset,
            Direction face,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof EmberBlock) {
            cir.setReturnValue(true);
        }
    }
}
