package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunClockRate;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientClockManager.class)
public class MunClockRateMixin implements MunClockRate {
    @Unique
    private float anvilcraft$rate = 1;

    @Inject(method = "handleUpdates", at = @At("TAIL"))
    private void anvilcraft$rememberClockRate(long time, Map<Holder<WorldClock>, ClockNetworkState> updates, CallbackInfo ci) {
        updates.forEach((clock, state) -> {
            if (clock.is(WorldClocks.OVERWORLD)) this.anvilcraft$rate = state.rate();
        });
    }

    @Override
    public float anvilcraft$overworldClockRate() {
        return this.anvilcraft$rate;
    }
}
