package dev.dubhe.anvilcraft.client.renderer.mun;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class MunShadowMathChecks {
    static void verify() {
        var clock = new MunShadowClock();
        check(clock.update(13, 0.3, 0.8) && clock.dayTime() == 12 && clock.partialTick() == 0, "Initial high-sun quantization");
        check(!clock.update(14, 0.7, 0.8) && clock.dayTime() == 12, "High-sun clock moved within one interval");
        check(!clock.update(16, 0, 0.8) && clock.dayTime() == 16, "Forward clock step");
        check(clock.update(15, 0, 0.8) && clock.dayTime() == 12, "Reverse motion must invalidate history");
        check(clock.update(100, 0.63, 0.04) && clock.dayTime() == 100 && clock.partialTick() == 0.5, "Horizon quarter ticks");
        check(!clock.update(100, 0.63, 0.04), "Paused time invalidated history");
        clock.clear();
        check(clock.update(-1, 0.5, 0.8) && clock.dayTime() == -4, "Negative time must use floor division");
        clock.clear();
        check(clock.update(100, -0.25, 0.04) && clock.dayTime() == 99 && clock.partialTick() == 0.75, "Negative fractional normalization");
        var projection = new MunShadowProjection();
        Vec3 origin = new Vec3(1000000, 100, -1000000);
        Vec3 anchor = origin.add(0.1, 0, 0.1);
        check(projection.update(anchor, origin, 64, 2048), "Initial projection update");
        check(!projection.update(anchor.add(0.001, 0, 0.001), origin, 64, 2048), "Sub-texel motion changed the shadow grid");
        check(projection.update(anchor.add(0.1, 0, 0), origin, 64, 2048), "A texel crossing did not move the grid");
        Vector3f translation = projection.matrix().transformPosition(new Vector3f());
        check(translation.distance(projection.offset(origin)) < 0.000001, "Per-mesh offset and full projection disagree");
        check(projection.intersects(new AABB(origin.x - 1, 60, origin.z - 1, origin.x + 1, 70, origin.z + 1)),
            "Near projected bounds were culled");
        check(!projection.intersects(new AABB(origin.x + 100, 60, origin.z, origin.x + 101, 70, origin.z + 1)),
            "Off-window bounds were admitted");
        check(!projection.intersects(new AABB(origin.x, 900, origin.z, origin.x + 1, 910, origin.z + 1)),
            "Out-of-depth bounds were admitted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
