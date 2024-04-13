package dev.dubhe.anvilcraft.mixin.fabric;

import dev.dubhe.anvilcraft.api.event.fabric.LightningBoltEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
abstract class LightningBoltMixin {
    @Shadow
    protected abstract BlockPos getStrikePosition();

    @SuppressWarnings("UnreachableCode")
    @Inject(method = "powerLightningRod", at = @At("HEAD"))
    private void powerLightningRod(CallbackInfo ci) {
        LightningBolt bolt = (LightningBolt) (Object) this;
        LightningBoltEvent.LIGHTNING_STRIKE.invoker().strike(bolt.level(), this.getStrikePosition(), bolt);
    }
}
