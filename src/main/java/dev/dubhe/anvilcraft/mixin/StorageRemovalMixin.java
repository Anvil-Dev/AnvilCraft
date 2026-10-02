package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
abstract class StorageRemovalMixin {
    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/entity/BlockEntity;preRemoveSideEffects(Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void anvilcraft$dropCrateContents(
        BlockEntity entity, BlockPos pos, BlockState state, Operation<Void> original, @Local(argsOnly = true) int flags
    ) {
        var level = entity.getLevel();
        if (level != null) {
            if (entity instanceof CrateBlockEntity crate) {
                crate.dropContents(level, pos);
            } else if ((flags & Block.UPDATE_MOVE_BY_PISTON) == 0 && state.getBlock() instanceof LargeCrateBlock block) {
                BlockPos mainPos = block.getMainPartPos(pos, state);
                if (level.getBlockEntity(mainPos) instanceof LargeCrateBlockEntity crate) crate.dropContents(level, mainPos);
            }
        }
        original.call(entity, pos, state);
    }
}
