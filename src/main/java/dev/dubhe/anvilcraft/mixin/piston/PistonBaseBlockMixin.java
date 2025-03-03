package dev.dubhe.anvilcraft.mixin.piston;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.util.mixin.magic.PistonMovingBlockEntityInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Debug(export = true)
@Mixin(PistonBaseBlock.class)
abstract class PistonBaseBlockMixin {
    @Unique
    private CompoundTag anvilcraft$nbt;

    @Redirect(method = "isPushable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"))
    private static boolean isPushable(@NotNull BlockState instance) {
        return instance.hasBlockEntity() && !(instance.getBlock() instanceof IMoveableEntityBlock);
    }

    @Inject(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            ordinal = 1
        )
    )
    private void setBlock(
        @NotNull Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir,
        @Local(ordinal = 2) BlockPos blockpos,
        @Local(ordinal = 1) Direction direction,
        @Local(ordinal = 1) @NotNull List<BlockState> list1,
        @Local(ordinal = 1) int k
    ) {
        if (level.isClientSide()) return;
        this.anvilcraft$nbt = new CompoundTag();
        if (list1.get(k).getBlock() instanceof IMoveableEntityBlock block) {
            this.anvilcraft$nbt = block.clearData(level, blockpos.relative(direction.getOpposite()));
        }
    }

    @Redirect(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            ordinal = 0
        )
    )
    private @NotNull BlockEntity newMovingBlockEntity(
        BlockPos pos,
        BlockState blockState,
        BlockState movedState,
        Direction direction,
        boolean extending,
        boolean isSourcePiston
    ) {
        BlockEntity blockEntity = MovingPistonBlock.newMovingBlockEntity(pos, blockState, movedState, direction, extending, isSourcePiston);
        if (blockEntity instanceof PistonMovingBlockEntityInjector entity)
            entity.anvilcraft$setData(this.anvilcraft$nbt);
        return blockEntity;
    }
}
