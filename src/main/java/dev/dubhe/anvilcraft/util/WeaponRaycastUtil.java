package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        return WeaponRaycastUtil.visualStart(player, 1.0F, rightOffset);
    }

    public static Vec3 visualStart(Player player, float partialTick, double rightOffset) {
        return WeaponRaycastUtil.visualStart(player, player.getEyePosition(partialTick), partialTick, rightOffset);
    }

    public static Vec3 visualStart(Player player, Vec3 eyePosition, float partialTick, double rightOffset) {
        Vec3 look = player.getViewVector(partialTick);
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        if (right.lengthSqr() < 1.0E-6) {
            right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        HumanoidArm usedArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
            ? player.getMainArm()
            : player.getMainArm().getOpposite();
        double side = usedArm == HumanoidArm.RIGHT ? 1.0 : -1.0;
        return eyePosition
            .add(look.scale(WeaponRaycastUtil.MUZZLE_FORWARD_OFFSET))
            .add(right.scale(rightOffset * side))
            .add(up.scale(-WeaponRaycastUtil.MUZZLE_DOWN_OFFSET));
    }

    public static Vec3 blockEnd(Level level, Entity source, Ray ray) {
        BlockHitResult hit = level.clip(new ClipContext(
            ray.start(), ray.end(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return hit.getType() == HitResult.Type.MISS ? ray.end() : hit.getLocation();
    }

    public static BlockHitResult laserBlockHit(Level level, Entity source, Ray ray) {
        ClipContext context = new ClipContext(
            ray.start(), ray.end(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source);
        return BlockGetter.traverseBlocks(
            ray.start(),
            ray.end(),
            context,
            (clipContext, pos) -> {
                BlockState state = level.getBlockState(pos);
                if (state.is(ModBlockTags.LASER_CAN_PASS_THROUGH)) return null;
                return level.clipWithInteractionOverride(
                    ray.start(), ray.end(), pos, clipContext.getBlockShape(state, level, pos), state);
            },
            clipContext -> BlockHitResult.miss(
                ray.end(),
                Direction.getApproximateNearest(ray.end().subtract(ray.start())),
                BlockPos.containing(ray.end())
            )
        );
    }

    public static @Nullable EntityHitResult firstEntity(
        Level level,
        Entity source,
        Ray ray,
        Predicate<Entity> predicate
    ) {
        Vec3 end = WeaponRaycastUtil.blockEnd(level, source, ray);
        return ProjectileUtil.getEntityHitResult(
            source,
            ray.start(),
            end,
            source.getBoundingBox().expandTowards(end.subtract(ray.start())).inflate(1.0),
            entity -> entity != source && entity.isPickable() && predicate.test(entity),
            Double.MAX_VALUE
        );
    }

    public static List<LivingEntity> livingEntities(Level level, Entity source, Ray ray, int limit) {
        Vec3 end = WeaponRaycastUtil.blockEnd(level, source, ray);
        return WeaponRaycastUtil.livingEntitiesToEnd(level, source, new Ray(ray.start(), end), limit);
    }

    public static List<LivingEntity> livingEntitiesToEnd(Level level, Entity source, Ray ray, int limit) {
        Vec3 end = ray.end();
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
