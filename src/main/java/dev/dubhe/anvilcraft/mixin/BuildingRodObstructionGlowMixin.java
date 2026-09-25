package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.building.BuildingRodObstructionHighlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class BuildingRodObstructionGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$highlightBuildingObstruction(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BuildingRodObstructionHighlight.isHighlighted(entity)) cir.setReturnValue(true);
    }
}
