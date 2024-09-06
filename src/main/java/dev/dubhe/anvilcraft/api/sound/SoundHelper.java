package dev.dubhe.anvilcraft.api.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class SoundHelper {
    public static SoundHelper INSTANCE = new SoundHelper();

    private final List<SoundEventListener> eventListeners = new CopyOnWriteArrayList<>();

    public boolean shouldPlay(ResourceLocation sound, Vec3 pos) {
        return eventListeners.stream().allMatch(it -> it.shouldPlay(sound, pos));
    }

    public void register(SoundEventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public void unregister(SoundEventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    public void clear() {
        eventListeners.clear();
    }
}
