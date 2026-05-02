package dev.dubhe.anvilcraft.api.sound;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * 声音事件监听器
 */
public interface ISoundEventListener {
    boolean shouldMute(Identifier sound, Vec3 pos);
}
