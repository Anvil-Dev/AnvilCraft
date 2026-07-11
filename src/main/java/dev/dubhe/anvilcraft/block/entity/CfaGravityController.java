package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 管理单个锻星砧天体的重力源和天体表面碰撞伤害。
 */
final class CfaGravityController {
    private static final int BASE_GRAVITY_RADIUS = 4;
    private static final int DEFAULT_CENTER_Y = 6;

    private boolean active;
    private double strength;
    private int radius = BASE_GRAVITY_RADIUS;
    private int centerY = DEFAULT_CENTER_Y;
    private int bodySize;
    private double bodyRadius;

    void tick(
        @Nullable Level level,
        BlockPos controllerPos,
        boolean amplified,
        @Nullable CelestialBodyData body,
        int stellarMass,
        int redstoneSignal
    ) {
        if (level == null || level.isClientSide()) return;
        boolean shouldBeActive = body != null && stellarMass > 0 && body.size() > 0;
        if (!shouldBeActive) {
            this.remove(level, controllerPos);
            return;
        }

        int signal = Math.max(0, Math.min(15, redstoneSignal));
        double targetBodyRadius = calculateBodyRadius(body, signal);
        int targetRadius = calculateGravityRadius(body, amplified, signal);
        int targetCenterY = calculateCenterY(body, amplified, signal);
        double targetStrength = calculateStrength(body, stellarMass, targetBodyRadius);
        BlockPos oldCenter = controllerPos.offset(0, this.centerY, 0);
        BlockPos newCenter = controllerPos.offset(0, targetCenterY, 0);

        if (!this.active
            || targetStrength != this.strength
            || targetRadius != this.radius
            || targetCenterY != this.centerY
            || body.size() != this.bodySize
            || targetBodyRadius != this.bodyRadius) {
            if (this.active) {
                GravityManager.GravitySourceManager.removeSource(level, oldCenter);
            }
            GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(
                targetStrength,
                targetRadius,
                targetBodyRadius
            );
            GravityManager.GravitySourceManager.addSource(level, newCenter, type);
            this.active = true;
            this.strength = targetStrength;
            this.radius = targetRadius;
            this.centerY = targetCenterY;
            this.bodySize = body.size();
            this.bodyRadius = targetBodyRadius;
        }

        this.destroyEntitiesInsideBody(level, controllerPos, amplified, signal, body);
    }

    void remove(@Nullable Level level, BlockPos controllerPos) {
        if (level == null || level.isClientSide() || !this.active) return;
        GravityManager.GravitySourceManager.removeSource(level, controllerPos.offset(0, this.centerY, 0));
        this.active = false;
        this.strength = 0.0;
        this.radius = BASE_GRAVITY_RADIUS;
        this.centerY = DEFAULT_CENTER_Y;
        this.bodySize = 0;
        this.bodyRadius = 0.0;
    }

    private static double calculateStrength(CelestialBodyData body, int stellarMass, double targetBodyRadius) {
        double massRatio = Math.pow(2.0, (stellarMass - 12) / 2.0);
        double targetStrength = Math.log1p(massRatio) / Math.log(2.0);
        double unscaledBodyRadius = body.bodyScale() / 2.0;
        if (unscaledBodyRadius > 1.0E-6 && targetBodyRadius > 1.0E-6) {
            double scaleRatio = targetBodyRadius / unscaledBodyRadius;
            targetStrength *= scaleRatio * scaleRatio;
        }
        return targetStrength;
    }

    private static int calculateGravityRadius(CelestialBodyData body, boolean amplified, int redstoneSignal) {
        float redstoneFactor = redstoneSignal / 5.0f;
        float fullRingScale = CelestialBodyData.ringSystemScale(body, amplified);
        float ringScale = CelestialBodyData.BASE_RING_SCALE
            + (fullRingScale - CelestialBodyData.BASE_RING_SCALE) * redstoneFactor;
        return Math.max(
            1,
            Math.round(BASE_GRAVITY_RADIUS * ringScale / CelestialBodyData.BASE_RING_SCALE)
        );
    }

    private static int calculateCenterY(CelestialBodyData body, boolean amplified, int redstoneSignal) {
        return Math.round(calculateVisualCenterY(body, amplified, redstoneSignal));
    }

    private static float calculateVisualCenterY(CelestialBodyData body, boolean amplified, int redstoneSignal) {
        float redstoneFactor = redstoneSignal / 5.0f;
        float baseCenterY = amplified ? 6.5f : 4.5f;
        float fullCenterY = CelestialBodyData.dynamicCenterY(body, amplified);
        float visualCenterY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;
        if (amplified) {
            visualCenterY += 19.0f * (redstoneSignal / 15.0f);
        }
        return visualCenterY;
    }

    private static double calculateBodyRadius(CelestialBodyData body, int redstoneSignal) {
        float redstoneFactor = redstoneSignal / 5.0f;
        float rawBodyScale = body.bodyScale();
        float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
        double visualRadius = (rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor) / 2.0;
        if (body instanceof SpecialCelestialBodyData special && special.isErrorPlanet()) {
            visualRadius *= 0.25;
        }
        return visualRadius;
    }

    private void destroyEntitiesInsideBody(
        Level level,
        BlockPos controllerPos,
        boolean amplified,
        int redstoneSignal,
        CelestialBodyData body
    ) {
        if (this.bodyRadius <= 0.0) return;
        double centerX = controllerPos.getX() + 0.5;
        double centerY = controllerPos.getY() + calculateVisualCenterY(body, amplified, redstoneSignal);
        double centerZ = controllerPos.getZ() + 0.5;
        double radiusSquare = this.bodyRadius * this.bodyRadius;
        AABB bodyBounds = new AABB(
            centerX - this.bodyRadius,
            centerY - this.bodyRadius,
            centerZ - this.bodyRadius,
            centerX + this.bodyRadius,
            centerY + this.bodyRadius,
            centerZ + this.bodyRadius
        );
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, bodyBounds);
        for (Entity entity : entities) {
            Vec3 entityCenter = entity.getBoundingBox().getCenter();
            double dx = entityCenter.x - centerX;
            double dy = entityCenter.y - centerY;
            double dz = entityCenter.z - centerZ;
            if (dx * dx + dy * dy + dz * dz > radiusSquare) continue;
            if (entity instanceof LivingEntity living) {
                applyCelestialDamage(level, body, living);
            } else {
                entity.discard();
            }
        }
    }

    private static void applyCelestialDamage(Level level, CelestialBodyData body, LivingEntity living) {
        if (body instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                // noinspection deprecation
                living.hurtOrSimulate(ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
            } else {
                // noinspection deprecation
                living.hurtOrSimulate(level.damageSources().inFire(), Float.MAX_VALUE);
            }
        } else {
            // noinspection deprecation
            living.hurtOrSimulate(level.damageSources().fall(), Float.MAX_VALUE);
        }
    }
}
