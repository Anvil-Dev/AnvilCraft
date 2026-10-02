package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.building.BuildingCommit;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelTicks.class)
public class BlueprintTicksMixin {
    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$quietBlueprintTicks(CallbackInfo ci) {
        var level = BuildingCommit.quietLevel();
        if (level != null && ((Object) this == level.getBlockTicks() || (Object) this == level.getFluidTicks())) ci.cancel();
    }
}
