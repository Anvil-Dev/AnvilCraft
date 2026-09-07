package dev.dubhe.anvilcraft.util;

import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;
import javax.annotation.Nullable;

/** Subdivides one tick without invoking entity movement or collision code. */
public final class OrbitalIntegrator {
    private static final double XI = 0.1786178958448091;
    private static final double LAMBDA = -0.2123418310626054;
    private static final double CHI = -0.06626458266981849;
    private static final double[] DRIFT_WEIGHTS = {XI, CHI, 1 - 2 * (CHI + XI), CHI};
    private static final double[] KICK_WEIGHTS = {(1 - 2 * LAMBDA) / 2, LAMBDA, LAMBDA, (1 - 2 * LAMBDA) / 2};

    private OrbitalIntegrator() {
    }

    /**
     * Fourth-order PEFRL splitting preserves central-force angular momentum with four force samples per substep.
     * Coefficients: Omelyan et al., Phys. Rev. E 65, 056706 (2002).
     */
    public static @Nullable Step integrate(
        Vec3 position,
        Vec3 velocity,
        int substeps,
        BiFunction<Vec3, Vec3, Vec3> acceleration
    ) {
        if (substeps < 2 || substeps > 64 || !isFinite(position) || !isFinite(velocity)) return null;
        Vec3 displacement = Vec3.ZERO;
        double step = 1.0 / substeps;
        for (int index = 0; index < substeps; index++) {
            for (int stage = 0; stage < DRIFT_WEIGHTS.length; stage++) {
                displacement = displacement.add(velocity.scale(step * DRIFT_WEIGHTS[stage]));
                Vec3 force = acceleration.apply(position.add(displacement), velocity);
                velocity = velocity.add(force.scale(step * KICK_WEIGHTS[stage]));
                if (!isFinite(displacement) || !isFinite(velocity)) return null;
            }
            displacement = displacement.add(velocity.scale(step * XI));
            if (!isFinite(displacement)) return null;
        }
        return new Step(displacement, velocity);
    }

    /** Schwarzschild orbit correction, with h = r cross v and a gameplay cap on the extra attraction. */
    public static double relativisticFactor(Vec3 offset, Vec3 velocity, double inverseLightSpeedSquared) {
        double radiusSquared = offset.lengthSqr();
        if (radiusSquared < 1 || inverseLightSpeedSquared <= 0) return 1;
        double angularMomentumSquared = offset.cross(velocity).lengthSqr();
        return 1 + Math.min(0.01, 3 * angularMomentumSquared * inverseLightSpeedSquared / radiusSquared);
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    public record Step(Vec3 movement, Vec3 velocity) {
    }
}
