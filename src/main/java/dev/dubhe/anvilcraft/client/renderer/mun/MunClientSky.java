package dev.dubhe.anvilcraft.client.renderer.mun;

import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class MunClientSky {
    private static @Nullable ClientLevel cachedLevel;
    private static long cachedTime = Long.MIN_VALUE;
    private static Vec3 cachedPosition = Vec3.ZERO;
    private static float cachedSunlight;

    private MunClientSky() {
    }

    public static float sunlight(ClientLevel level, Vec3 position) {
        long time = level.getOverworldClockTime();
        if (cachedLevel != level || cachedTime != time || !cachedPosition.equals(position)) {
            cachedLevel = level;
            cachedTime = time;
            cachedPosition = position;
            cachedSunlight = (float) MunSkyMath.sunlight(position.x, position.z, time, 0);
        }
        return cachedSunlight;
    }

    public static void clear() {
        cachedLevel = null;
        cachedTime = Long.MIN_VALUE;
        cachedPosition = Vec3.ZERO;
        cachedSunlight = 0;
    }

    public static double partialDayTime(ClientLevel level, float partialTick) {
        float rate = ((MunClockRate) level.clockManager()).anvilcraft$overworldClockRate();
        if (rate == 0) return 0;
        if (rate < 0) return partialTick;
        var clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        return level.clockManager().getPartialTick(clock) + partialTick * rate;
    }
}
