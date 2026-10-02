package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.building.BuildingRodUndo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class BlueprintUndoChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void anvilcraft$trackReplacement(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        BlockState previous = cir.getReturnValue();
        if (previous != null) BuildingRodUndo.replaced(((LevelChunk) (Object) this).getLevel(), pos, previous, state);
    }
}
