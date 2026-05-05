package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At(value = "HEAD"), cancellable = true)
    private void onSoundPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (SoundHelper.INSTANCE.shouldMute(
            Minecraft.getInstance().level,
            Objects.requireNonNull(instance.getSound()).getLocation(),
            new Vec3(instance.getX(), instance.getY(), instance.getZ())
        )) {
            cir.cancel();
        }
    }
}
