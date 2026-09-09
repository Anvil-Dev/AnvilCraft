package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class MunClientSky {
    private static @Nullable ClientLevel cachedLevel;
    private static long cachedTime = Long.MIN_VALUE;
    private static Vec3 cachedPosition = Vec3.ZERO;
    private static float cachedSunlight;

    private MunClientSky() {
    }

    public static boolean isMun() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension().equals(CelestialTravelManager.MUN_LEVEL);
    }

    public static double partialDayTime(ClientLevel level, float partialTick) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return 0;
        float speed = level.getDayTimePerTick();
        return speed < 0 ? partialTick : level.getDayTimeFraction() + partialTick * speed;
    }

    public static float sunlight(ClientLevel level) {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (cachedLevel != level || cachedTime != level.getDayTime() || !cachedPosition.equals(position)) {
            cachedLevel = level;
            cachedTime = level.getDayTime();
            cachedPosition = position;
            cachedSunlight = (float) MunSkyMath.sunlight(position.x, position.z, cachedTime, 0);
        }
        return cachedSunlight;
    }

    public static void clear() {
        cachedLevel = null;
        cachedTime = Long.MIN_VALUE;
        cachedSunlight = 0;
    }
}
