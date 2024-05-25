package dev.dubhe.anvilcraft.api.sound;

import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * 声音事件监听器
 */
public interface SoundEventListener {
    boolean shouldPlay(SoundInstance instance);
}
