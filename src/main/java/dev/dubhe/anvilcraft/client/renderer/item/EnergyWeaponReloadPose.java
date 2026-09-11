package dev.dubhe.anvilcraft.client.renderer.item;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EnergyWeaponReloadPose {
    private static final float SCALE = 0.65F;
    private static final float CAPACITOR_SCALE = 0.42F;
    // Shared platform rear face: [8, 7 - sqrt(0.5), 21], in centered item-model coordinates.
    private static final float REAR_Y = (7 - (float) Math.sqrt(0.5) - 8) / 16;
    private static final float REAR_Z = 13.0F / 16;

    private EnergyWeaponReloadPose() {
    }

    public static Matrix4f holding(Vector3f muzzle, Vector3f target, float muzzleZ, float roll) {
        return new Matrix4f().translation(muzzle)
            .rotate(new Quaternionf().rotationTo(new Vector3f(0, 0, -1), new Vector3f(target).sub(muzzle).normalize()))
            .rotateZ(radians(roll))
            .scale(0.55F)
            .translate(0, 1.0F / 16, (8 - muzzleZ) / 16);
    }

    public static Matrix4f weapon(float elapsed, float side) {
        return weapon(holding(new Vector3f(side * 0.28F, -0.2F, -1.1F), new Vector3f(0, 0, -64), -5, 0), elapsed, side);
    }

    public static Matrix4f weapon(Matrix4f holding, float elapsed, float side) {
        float lift = lift(elapsed);
        Matrix4f charging = new Matrix4f()
            .translation(side * 0.44F, -0.10F, -0.72F)
            .rotateZ(radians(-side * 8))
            .rotateY(radians(-side * 65))
            .rotateX(radians(15))
            .scale(SCALE);
        return blend(holding, charging, lift);
    }

    public static Matrix4f weapon(Matrix4f holding, Matrix4f attacking, float elapsed, float side, float attackBlend) {
        return blend(weapon(holding, elapsed, side), attacking, attackBlend);
    }

    public static float attackMuzzleDepth(float elapsed) {
        return 1.1F + 0.25F * lift(elapsed);
    }

    public static float approachAttack(float current, boolean attacking, float milliseconds) {
        float target = attacking ? 1 : 0;
        float duration = attacking ? 40 : 70;
        return target + (current - target) * (float) Math.exp(-Math.clamp(milliseconds, 0, 100) / duration);
    }

    public static Vector3f capacitorGrip(Matrix4f capacitor, float side) {
        return capacitor.transformPosition(new Vector3f(-side * 0.65F, 0.35F, 1.25F));
    }

    public static Vector3f weaponGrip(Matrix4f weapon, float side) {
        return weapon.transformPosition(new Vector3f(0, -5.0F / 16, 3.0F / 16)).add(side * 0.045F, -0.045F, 0);
    }

    public static boolean showsCapacitor(float elapsed) {
        return elapsed >= 8 && elapsed < 34;
    }

    public static Matrix4f capacitor(float elapsed, float side, boolean regular) {
        float reach = smooth((elapsed - 4) / 11);
        float retract = smooth((elapsed - 29) / 9);
        float away = 1 - reach * (1 - retract);
        float insert = smooth((elapsed - 15) / 3) * (1 - smooth((elapsed - 26) / 3));
        float arc = (float) Math.sin(away * Math.PI);
        return new Matrix4f()
            .translation(-side * (away * 1.1F + arc * 0.15F), REAR_Y - away * away * 1.3F,
                REAR_Z + 0.30F * (1 - insert) + arc * 0.14F + away * 1.6F)
            .rotateY(radians(-side * away * 35))
            .rotateX(radians(90 - away * 65))
            .scale(CAPACITOR_SCALE)
            // Anchor the bottom face, not the item center; normal and super capacitors have different base heights.
            .translate(0, regular ? 7.0F / 16 : 0.5F, 0);
    }

    public static Matrix4f capacitor(float elapsed, float side, boolean regular, float attackBlend) {
        float reach = smooth((elapsed - 4) / 11);
        float retract = smooth((elapsed - 29) / 9);
        float away = 1 - reach * (1 - retract);
        float insert = smooth((elapsed - 15) / 3) * (1 - smooth((elapsed - 26) / 3));
        Matrix4f attacking = new Matrix4f()
            .translation(-side * away * 0.8F, REAR_Y - away * 0.65F, REAR_Z + 0.08F * (1 - insert))
            .rotateY(radians(-side * away * 20))
            .rotateX(radians(90))
            .scale(CAPACITOR_SCALE)
            .translate(0, regular ? 7.0F / 16 : 0.5F, 0);
        return blend(capacitor(elapsed, side, regular), attacking, attackBlend);
    }

    public static Matrix4f capacitor(
        Matrix4f holding, Matrix4f attacking, float elapsed, float side, boolean regular, float attackBlend
    ) {
        Matrix4f normal = weapon(holding, elapsed, side).mul(capacitor(elapsed, side, regular));
        Matrix4f combat = new Matrix4f(attacking).mul(capacitor(elapsed, side, regular, 1));
        Matrix4f free = blend(normal, combat, attackBlend);
        // Blend the draw/stow paths in camera space so a turning gun cannot sweep the capacitor through the camera.
        float contact = smooth((elapsed - 14) / 4) * (1 - smooth((elapsed - 26) / 3));
        if (contact <= 0) return free;
        Matrix4f attached = weapon(holding, attacking, elapsed, side, attackBlend)
            .mul(capacitor(elapsed, side, regular, attackBlend));
        return blend(free, attached, contact);
    }

    public static Matrix4f arm(Vector3f grip, float side) {
        Vector3f towardShoulder = new Vector3f(side * 0.6F, -0.5F, 1);
        return new Matrix4f().translation(grip)
            .rotate(new Quaternionf().rotationTo(new Vector3f(0, -1, 0), towardShoulder.normalize()));
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static float lift(float elapsed) {
        return smooth(elapsed / 8) * (1 - smooth((elapsed - 32) / 8));
    }

    private static Matrix4f blend(Matrix4f from, Matrix4f to, float progress) {
        float amount = Math.clamp(progress, 0, 1);
        Vector3f position = from.getTranslation(new Vector3f()).lerp(to.getTranslation(new Vector3f()), amount);
        Quaternionf rotation = from.getUnnormalizedRotation(new Quaternionf())
            .slerp(to.getUnnormalizedRotation(new Quaternionf()), amount);
        float scale = from.getScale(new Vector3f()).x;
        float targetScale = to.getScale(new Vector3f()).x;
        return new Matrix4f().translation(position).rotate(rotation).scale(scale + (targetScale - scale) * amount);
    }

    private static float smooth(float value) {
        float progress = Math.clamp(value, 0, 1);
        return progress * progress * (3 - 2 * progress);
    }
}
