package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockStructureResolver;
import dev.dubhe.anvilcraft.event.PistonMoveBlockListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PistonStructureResolver.class)
abstract class PistonStructureResolverMixin {
    @Shadow
    @Final
    private List<BlockPos> toPush;

    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private Direction pushDirection;

    @Inject(method = "resolve", at = @At("RETURN"))
    private void onPistonResolve(CallbackInfoReturnable<Boolean> cir) {
        if (this.level.isClientSide()) {
            return;
        }
        if (!cir.getReturnValue()) return;
        List<BlockPos> toPushBlocks = new ArrayList<>(this.toPush);
        PistonMoveBlockListener.onPistonMoveBlocks(this.level, toPushBlocks);
    }

    @ModifyConstant(method = "addBlockLine", constant = @Constant(intValue = 12, ordinal = 0))
    private int updateMaxPushDepth(int constant) {
        SlidingBlockStructureResolver.MAX_PUSH_DEPTH = constant;
        return constant;
    }

    @WrapOperation(
        method = "addBlockLine",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0
        )
    )
    private boolean useEnhancedCheck00(
        BlockState instance,
        BlockState state,
        Operation<Boolean> original,
        @Local(name = "pos") BlockPos pos
    ) {
        BlockPos pos = pos.relative(this.pushDirection);
        return instance.anvilcraft$canStickTo(pos, pos, state);
    }

    @WrapOperation(
        method = "addBlockLine",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 1
        )
    )
    private boolean useEnhancedCheck01(
        BlockState instance, BlockState state, Operation<Boolean> original, @Local(name = "pos") BlockPos pos
    ) {
        BlockPos otherPos = pos.relative(this.pushDirection);
        return instance.anvilcraft$canStickTo(pos, otherPos, state);
    }

    @WrapOperation(
        method = "addBranchingBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0
        )
    )
    private boolean useEnhancedCheck10(
        BlockState instance,
        BlockState state,
        Operation<Boolean> original,
        @Local(argsOnly = true, name = "fromPos") BlockPos fromPos,
        @Local(name = "neighbourPos") BlockPos neighbourPos
    ) {
        return instance.anvilcraft$canStickTo(neighbourPos, fromPos, state);
    }

    @WrapOperation(
        method = "addBranchingBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 1
        )
    )
    private boolean useEnhancedCheck11(
        BlockState instance,
        BlockState state,
        Operation<Boolean> original,
        @Local(argsOnly = true, name = "fromPos") BlockPos fromPos,
        @Local(name = "neighbourPos") BlockPos neighbourPos
    ) {
        return instance.anvilcraft$canStickTo(fromPos, neighbourPos, state);
    }
}
