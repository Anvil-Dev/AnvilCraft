package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(
            method = "play",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onSoundPlay(SoundInstance sound, CallbackInfo ci) {
        if (!SoundHelper.INSTANCE.shouldPlay(sound)) {
            ci.cancel();
        }
    }
}
