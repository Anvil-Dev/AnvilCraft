package dev.dubhe.anvilcraft.client.building;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class BuildingRodObstructionHighlight {
    private static final Map<Entity, Long> EXPIRES_AT = new WeakHashMap<>();

    private BuildingRodObstructionHighlight() {
    }

    public static void show(Level level, List<Integer> ids) {
        long now = System.nanoTime();
        EXPIRES_AT.entrySet().removeIf(entry -> entry.getKey().level() != level || entry.getValue() <= now);
        for (int id : ids) {
            Entity entity = level.getEntity(id);
            if (entity != null) EXPIRES_AT.put(entity, now + 3_000_000_000L);
        }
    }

    public static boolean isHighlighted(Entity entity) {
        Long expiresAt = EXPIRES_AT.get(entity);
        if (expiresAt == null) return false;
        if (System.nanoTime() < expiresAt) return true;
        EXPIRES_AT.remove(entity);
        return false;
    }
}
