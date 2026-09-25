package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.building.BuildingCommit;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class BlueprintServerLevelMixin {
    @Inject(method = {"updateNeighborsAt", "updateNeighborsAtExceptFromFacing", "neighborChanged", "blockEvent"},
        at = @At("HEAD"), cancellable = true)
    private void anvilcraft$quietBlueprintNeighbors(CallbackInfo ci) {
        if (BuildingCommit.isQuiet((ServerLevel) (Object) this)) ci.cancel();
    }
}
