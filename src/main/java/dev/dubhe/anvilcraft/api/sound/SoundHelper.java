package dev.dubhe.anvilcraft.api.sound;

import lombok.Getter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

@Getter
public class SoundHelper {
    public static final SoundHelper INSTANCE = new SoundHelper();

    private final Map<ResourceKey<Level>, List<ISoundEventListener>> eventListeners = new HashMap<>();

    public boolean shouldMute(@Nullable Level level, ResourceLocation sound, Vec3 pos) {
        if (level == null) return false;
        return this.eventListeners.computeIfAbsent(level.dimension(), k -> new CopyOnWriteArrayList<>())
            .stream()
            .anyMatch(it -> it.shouldMute(sound, pos));
    }

    public void register(Level level, ISoundEventListener eventListener) {
        this.eventListeners.computeIfAbsent(level.dimension(), k -> new CopyOnWriteArrayList<>())
            .add(eventListener);
    }

    public void unregister(Level level, ISoundEventListener eventListener) {
        this.eventListeners.computeIfAbsent(level.dimension(), k -> new CopyOnWriteArrayList<>())
            .remove(eventListener);
    }

    public void clear() {
        this.eventListeners.clear();
    }
}
