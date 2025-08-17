package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Contraption.class)
public abstract class CreateContraptionMixin {
    @Redirect(
        method = "moveBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0))
    private boolean useEnhancedCheck0(
        BlockState instance, BlockState state, @Local(ordinal = 0) BlockPos pos, @Local(ordinal = 1) BlockPos otherPos
    ) {
        return instance.canStickTo(pos, otherPos, state);
    }

    @Redirect(
        method = "moveBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 1))
    private boolean useEnhancedCheck1(
        BlockState instance, BlockState state, @Local(ordinal = 0) BlockPos otherPos, @Local(ordinal = 1) BlockPos pos
    ) {
        return instance.canStickTo(pos, otherPos, state);
    }
}
