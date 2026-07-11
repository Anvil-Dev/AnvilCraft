package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class WeaponRaycastUtil {
    public static final double MUZZLE_FORWARD_OFFSET = 0.25;
    public static final double MUZZLE_RIGHT_OFFSET = 0.13;
    public static final double MUZZLE_DOWN_OFFSET = 0.1;

    private WeaponRaycastUtil() {
    }

    public static Ray ray(Player player, double range) {
        Vec3 start = player.getEyePosition();
        return new Ray(start, start.add(player.getViewVector(1.0F).scale(range)));
    }

    public static Vec3 visualStart(Player player, double rightOffset) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        if (right.lengthSqr() < 1.0E-6) {
            right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        return player.getEyePosition()
            .add(look.scale(MUZZLE_FORWARD_OFFSET))
            .add(right.scale(rightOffset))
            .add(up.scale(-MUZZLE_DOWN_OFFSET));
    }

    public static Vec3 blockEnd(Level level, Entity source, Ray ray) {
        BlockHitResult hit = level.clip(new ClipContext(
            ray.start(), ray.end(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return hit.getType() == HitResult.Type.MISS ? ray.end() : hit.getLocation();
    }

    public static EntityHitResult firstEntity(Level level, Entity source, Ray ray, Predicate<Entity> predicate) {
        Vec3 end = blockEnd(level, source, ray);
        return Objects.requireNonNull(ProjectileUtil.getEntityHitResult(
            level,
            source,
            ray.start(),
            end,
            source.getBoundingBox().expandTowards(end.subtract(ray.start())).inflate(1.0),
            entity -> entity != source && entity.isPickable() && predicate.test(entity)
        ));
    }

    public static List<LivingEntity> livingEntities(Level level, Entity source, Ray ray, int limit) {
        Vec3 end = blockEnd(level, source, ray);
        Vec3 direction = end.subtract(ray.start());
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(
            LivingEntity.class,
            source.getBoundingBox().expandTowards(direction).inflate(1.0),
            entity -> entity != source && entity.isAlive()
        )) {
            if (entity.getBoundingBox().inflate(0.3).clip(ray.start(), end).isPresent()) result.add(entity);
        }
        result.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(source)));
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    public record Ray(Vec3 start, Vec3 end) {
    }
}
