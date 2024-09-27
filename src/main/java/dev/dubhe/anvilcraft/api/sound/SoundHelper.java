package dev.dubhe.anvilcraft.api.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundHelper {
    public static SoundHelper INSTANCE = new SoundHelper();

    private final Map<ClientLevel, List<SoundEventListener>> eventListeners = new HashMap<>();

    public boolean shouldPlay(ResourceLocation sound, Vec3 pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return true;
        return eventListeners.computeIfAbsent(level, k -> new CopyOnWriteArrayList<>())
            .stream()
            .allMatch(it -> it.shouldPlay(sound, pos));
    }

    public void register(SoundEventListener eventListener) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        eventListeners.computeIfAbsent(level, k -> new CopyOnWriteArrayList<>())
            .add(eventListener);
    }

    public void unregister(SoundEventListener eventListener) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            eventListeners.values().forEach(list -> list.remove(eventListener));
            return;
        }
        eventListeners.computeIfAbsent(level, k -> new CopyOnWriteArrayList<>())
            .remove(eventListener);
    }

    public void clear() {
        eventListeners.clear();
    }
}
