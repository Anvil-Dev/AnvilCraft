package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.FlintBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
abstract class BlockBehaviourMixin {
    @Inject(
        method = "onPlace",
        at = @At("HEAD")
    )
    private void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (state.is(Blocks.IRON_BLOCK) || state.is(ModBlocks.HEAVY_IRON_BLOCK)) {
            if (movedByPiston) {
                FlintBlock.ignite(level, pos, false);
            }
        }
    }

    @Inject(
        method = "onRemove",
        at = @At("RETURN")
    )
    private void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        if (state.is(Blocks.IRON_BLOCK) || state.is(ModBlocks.HEAVY_IRON_BLOCK)) {
            if (movedByPiston) {
                FlintBlock.ignite(level, pos, false);
            }
        }
    }
}
