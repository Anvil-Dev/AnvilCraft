package dev.dubhe.anvilcraft.mixin.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.LightningStrikeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
abstract class LightningBoltMixin {
    @Shadow
    protected abstract BlockPos getStrikePosition();

    @Unique
    private final LightningBolt ths = (LightningBolt) (Object) this;

    @Inject(method = "powerLightningRod", at = @At("HEAD"))
    private void powerLightningRod(CallbackInfo ci) {
        LightningStrikeEvent event = new LightningStrikeEvent(ths, this.getStrikePosition(), ths.level());
        AnvilCraft.EVENT_BUS.post(event);
    }
}
