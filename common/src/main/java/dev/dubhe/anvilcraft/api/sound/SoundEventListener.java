package dev.dubhe.anvilcraft.api.sound;

import net.minecraft.client.resources.sounds.SoundInstance;

public interface SoundEventListener {
    boolean shouldPlay(SoundInstance instance);
}
