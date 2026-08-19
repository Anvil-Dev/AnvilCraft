package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Owns the physical gravity source and contact effects for one CFA.
 *
 * <p>The controller deliberately keeps the 1.21 geometry formulas.  The 26.1
 * implementation uses a different redstone interpolation divisor, which would
 * move the source outside the rendered body on this branch.</p>
 */
final class CfaGravityController {
    private static final int BASE_GRAVITY_RADIUS = 4;
    private static final float PLANET_CONTACT_DAMAGE = 38.0f;
    private static final float STAR_CONTACT_DAMAGE = 66.0f;

    private boolean active;
    private double strength;
    private int radius = BASE_GRAVITY_RADIUS;
    private double bodyRadius;
    private Vec3 center = Vec3.ZERO;

    void tick(
        @Nullable Level level,
        BlockPos controllerPos,
        boolean amplified,
        boolean amplifierPresent,
        @Nullable CelestialBodyData body,
        int stellarMass,
        int redstoneSignal
    ) {
        if (level == null || level.isClientSide()) return;

        boolean shouldBeActive = body != null
            && stellarMass > 0
            && body.size() > 0
            && (!(body instanceof StarData star) || amplifierPresent || star.specialRedDwarf());
        if (!shouldBeActive) {
            this.remove(level, controllerPos);
            return;
        }

        int signal = Math.clamp(redstoneSignal, 0, 15);
        double nextBodyRadius = computeBodyRadius(body, signal);
        int nextRadius = computeGravityRadius(body, amplified, signal);
        double nextStrength = computeStrength(body, stellarMass, nextBodyRadius);
        Vec3 nextCenter = computeCenter(controllerPos, body, amplified, signal);

        if (!this.active
            || Double.compare(this.strength, nextStrength) != 0
            || this.radius != nextRadius
            || Double.compare(this.bodyRadius, nextBodyRadius) != 0
            || !this.center.equals(nextCenter)) {
            GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(
                nextStrength,
                nextRadius,
                nextBodyRadius
            );
            GravityManager.GravitySourceManager.upsertSource(level, controllerPos, nextCenter, type);
            this.active = true;
            this.strength = nextStrength;
            this.radius = nextRadius;
            this.bodyRadius = nextBodyRadius;
            this.center = nextCenter;
        }

    }

    void remove(@Nullable Level level, BlockPos controllerPos) {
        if (level != null && !level.isClientSide() && this.active) {
            GravityManager.GravitySourceManager.removeSource(level, controllerPos);
        }
        this.active = false;
        this.strength = 0.0;
        this.radius = BASE_GRAVITY_RADIUS;
        this.bodyRadius = 0.0;
        this.center = Vec3.ZERO;
    }

    boolean isActive() {
        return this.active;
    }

    void handleEntityContact(
        @Nullable Level level,
        @Nullable CelestialBodyData body,
        Entity entity
    ) {
        if (!this.active
            || level == null
            || level.isClientSide()
            || entity.level() != level
            || entity.isRemoved()
            || isEternal(entity)) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            applyCelestialDamage(level, body, living);
        } else {
            entity.discard();
        }
    }

    private static boolean isEternal(Entity entity) {
        ItemStack stack = switch (entity) {
            case ItemEntity itemEntity -> itemEntity.getItem();
            case ThrownHeavyHalberdEntity halberd -> halberd.getWeaponItem();
            default -> ItemStack.EMPTY;
        };
        return stack.has(ModComponents.ETERNAL);
    }

    private static void applyCelestialDamage(
        Level level,
        @Nullable CelestialBodyData body,
        LivingEntity living
    ) {
        if (body instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                living.hurt(ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
            } else {
                living.hurt(level.damageSources().inFire(), STAR_CONTACT_DAMAGE);
            }
        } else {
            living.hurt(level.damageSources().fall(), PLANET_CONTACT_DAMAGE);
        }
    }

    private static double computeStrength(CelestialBodyData body, int stellarMass, double bodyRadius) {
        double massRatio = Math.pow(2.0, (stellarMass - 12) / 2.0);
        double strength = Math.log1p(massRatio) / Math.log(2.0);
        double unscaledRadius = body.bodyScale() / 2.0;
        if (unscaledRadius > 1.0E-6 && bodyRadius > 1.0E-6) {
            double scaleRatio = bodyRadius / unscaledRadius;
            strength *= scaleRatio * scaleRatio;
        }
        return strength;
    }

    private static Vec3 computeCenter(
        BlockPos controllerPos,
        CelestialBodyData body,
        boolean amplified,
        int redstoneSignal
    ) {
        float factor = redstoneSignal / 15.0f;
        float baseCenterY = amplified ? 6.5f : 4.5f;
        float fullCenterY = CelestialBodyData.dynamicCenterY(body, amplified);
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * factor;
        return new Vec3(
            controllerPos.getX() + 0.5,
            controllerPos.getY() + centerY,
            controllerPos.getZ() + 0.5
        );
    }

    private static int computeGravityRadius(
        CelestialBodyData body,
        boolean amplified,
        int redstoneSignal
    ) {
        float factor = redstoneSignal / 15.0f;
        float fullRingScale = CelestialBodyData.ringSystemScale(body, amplified);
        float ringScale = CelestialBodyData.BASE_RING_SCALE
            + (fullRingScale - CelestialBodyData.BASE_RING_SCALE) * factor;
        return Math.max(1, Math.round(BASE_GRAVITY_RADIUS * ringScale / CelestialBodyData.BASE_RING_SCALE));
    }

    private static double computeBodyRadius(CelestialBodyData body, int redstoneSignal) {
        float factor = redstoneSignal / 15.0f;
        float rawBodyScale = body.bodyScale();
        float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
        double bodyRadius = (rawBodyScale + (fullBodyScale - rawBodyScale) * factor) / 2.0;
        // Error planets render as a quarter-sized body; keep their physical
        // collision/gravity core aligned with that visual scale.
        if (body instanceof SpecialCelestialBodyData special && special.isErrorPlanet()) {
            bodyRadius *= 0.25;
        }
        return bodyRadius;
    }
}
