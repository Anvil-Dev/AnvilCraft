package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MunNightVisionMixin {
    @ModifyReturnValue(method = "getNightVisionScale", at = @At("RETURN"))
    private static float anvilcraft$disableMoonNightVision(float original, LivingEntity camera, float partialTick) {
        if (camera.level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) {
            return 0;
        }
        return original;
    }
}
