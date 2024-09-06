package dev.dubhe.anvilcraft.api.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 声音事件监听器
 */
public interface SoundEventListener {
    boolean shouldPlay(ResourceLocation sound, Vec3 pos);
}
