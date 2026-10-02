package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.RuinsStructure;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
abstract class RuinsRemovalMixin {
    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/entity/BlockEntity;preRemoveSideEffects(Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void anvilcraft$removeRuinsParts(
        BlockEntity entity, BlockPos pos, BlockState state, Operation<Void> original, @Local(argsOnly = true) int flags
    ) {
        if ((flags & Block.UPDATE_MOVE_BY_PISTON) == 0 && entity instanceof RuinsBlockEntity ruins && ruins.getLevel() != null) {
            RuinsStructure.destroyOthers(ruins.getLevel(), ruins, true, null);
        }
        original.call(entity, pos, state);
    }
}
