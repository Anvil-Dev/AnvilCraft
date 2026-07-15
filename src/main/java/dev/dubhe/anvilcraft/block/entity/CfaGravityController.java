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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 管理单个锻星砧天体的重力源和天体表面碰撞伤害。
 */
final class CfaGravityController {
    private static final int BASE_GRAVITY_RADIUS = 4;
    // 38 * 0.52 = 19.76 after Feather Falling IV.
    private static final float PLANET_CONTACT_DAMAGE = 38.0f;
    // 66 * 0.84 * 0.36 = 19.9584 after full Protection IV diamond armor.
    private static final float STAR_CONTACT_DAMAGE = 66.0f;

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
            && (!(body instanceof StarData) || amplifierPresent);
        if (!shouldBeActive) {
            this.remove(level, controllerPos);
            return;
        }

        int signal = Math.max(0, Math.min(15, redstoneSignal));
        double targetBodyRadius = calculateBodyRadius(body, signal);
        int targetRadius = calculateGravityRadius(body, amplified, signal);
        double targetStrength = calculateStrength(body, stellarMass, targetBodyRadius);
        this.center = new Vec3(
            controllerPos.getX() + 0.5,
            controllerPos.getY() + calculateVisualCenterY(body, amplified, signal),
            controllerPos.getZ() + 0.5
        );
        GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(
            targetStrength,
            targetRadius,
            targetBodyRadius
        );
        GravityManager.GravitySourceManager.upsertSource(level, controllerPos, this.center, type);
        this.bodyRadius = targetBodyRadius;

        this.destroyEntitiesInsideBody(level, body);
    }

    void remove(@Nullable Level level, BlockPos controllerPos) {
        if (level == null || level.isClientSide()) return;
        GravityManager.GravitySourceManager.removeSource(level, controllerPos);
        this.bodyRadius = 0.0;
        this.center = Vec3.ZERO;
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

    private void destroyEntitiesInsideBody(Level level, CelestialBodyData body) {
        if (this.bodyRadius <= 0.0) return;
        AABB bodyBounds = new AABB(
            this.center.x - this.bodyRadius,
            this.center.y - this.bodyRadius,
            this.center.z - this.bodyRadius,
            this.center.x + this.bodyRadius,
            this.center.y + this.bodyRadius,
            this.center.z + this.bodyRadius
        );
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, bodyBounds);
        for (Entity entity : entities) {
            if (!intersectsSphere(entity.getBoundingBox(), this.center, this.bodyRadius)) continue;
            if (isEternal(entity)) continue;
            if (entity instanceof LivingEntity living) {
                applyCelestialDamage(level, body, living);
            } else {
                entity.discard();
            }
        }
    }

    private static boolean isEternal(Entity entity) {
        ItemStack stack = switch (entity) {
            case ItemEntity itemEntity -> itemEntity.getItem();
            case ThrownHeavyHalberdEntity heavyHalberd -> heavyHalberd.getWeaponItem();
            default -> ItemStack.EMPTY;
        };
        return stack.has(ModComponents.ETERNAL);
    }

    private static boolean intersectsSphere(AABB box, Vec3 center, double radius) {
        double x = Math.max(box.minX, Math.min(center.x, box.maxX));
        double y = Math.max(box.minY, Math.min(center.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(center.z, box.maxZ));
        double dx = x - center.x;
        double dy = y - center.y;
        double dz = z - center.z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static void applyCelestialDamage(Level level, CelestialBodyData body, LivingEntity living) {
        if (body instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                // noinspection deprecation
                living.hurtOrSimulate(ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
            } else {
                // noinspection deprecation
                living.hurtOrSimulate(level.damageSources().inFire(), STAR_CONTACT_DAMAGE);
            }
        } else {
            // noinspection deprecation
            living.hurtOrSimulate(level.damageSources().fall(), PLANET_CONTACT_DAMAGE);
        }
    }
}
