package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.building.BuildingCommit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class BlueprintLevelMixin {
    @Inject(method = {"updateNeighbourForOutputSignal", "neighborShapeChanged"}, at = @At("HEAD"), cancellable = true)
    private void anvilcraft$quietBlueprintUpdates(CallbackInfo ci) {
        if (BuildingCommit.isQuiet((Level) (Object) this)) ci.cancel();
    }

    @Inject(method = "neighborShapeChanged", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$keepBlueprintObserverPhase(Direction direction, BlockState source, BlockPos pos,
                                                       BlockPos sourcePos, int flags, int recursion, CallbackInfo ci) {
        if (BuildingCommit.isRestoredObserverUpdate((Level) (Object) this, direction, source, pos, sourcePos)) ci.cancel();
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"), cancellable = true)
    private void anvilcraft$quietBlueprintState(BlockPos pos, BlockState state, int flags, int recursion,
                                               CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (!BuildingCommit.isQuiet(level)) return;
        boolean changed = level.isInWorldBounds(pos) && level.getBlockState(pos) != state;
        BuildingCommit.set(level, pos, state);
        cir.setReturnValue(changed);
    }
}
