package dev.dubhe.anvilcraft.api.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SoundInstance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Environment(EnvType.CLIENT)
public class SoundHelper {
    public static SoundHelper INSTANCE = new SoundHelper();

    private final List<SoundEventListener> eventListeners = new CopyOnWriteArrayList<>();

    public boolean shouldPlay(SoundInstance instance) {
        return eventListeners.stream().allMatch(it -> it.shouldPlay(instance));
    }

    public void register(SoundEventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public void unregister(SoundEventListener eventListener) {
        eventListeners.remove(eventListener);
    }
}
