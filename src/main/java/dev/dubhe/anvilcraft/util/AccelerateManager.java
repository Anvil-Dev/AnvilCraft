package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AccelerateManager {
    public static final double MAX_ACCELERATED_SPEED = 512.0;
    private static final double MAX_PLAYER_SPEED = 20.0;

    public static void handleAcceleration(Entity entity) {
        if (!canBeAccelerated(entity)) return;
        Vec3 currentMovement = entity.getDeltaMovement();
        Vec3 clampedMovement = clampMovement(entity, currentMovement);
        if (clampedMovement != currentMovement) entity.setDeltaMovement(clampedMovement);
        Level level = entity.level();
        Vec3 center = getMovementCenter(entity);
        BlockPos selectedRing = null;
        Direction selectedDirection = null;
        double bestAlignment = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : AccelerationRingBlockEntity.getAllBlocks(level)) {
            AABB aabb = AccelerationRingBlockEntity.getAABB(level, pos);
            if (aabb == null) continue;
            if (!aabb.contains(center)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isActiveAccelerationRing(state)) continue;
            Direction direction = state.getValue(AccelerationRingBlock.FACING);
            double alignment = entity.getDeltaMovement().dot(Vec3.atLowerCornerOf(direction.getNormal()));
            if (alignment <= bestAlignment) continue;
            selectedRing = pos;
            selectedDirection = direction;
            bestAlignment = alignment;
        }
        if (selectedRing != null) applyAcceleration(entity, selectedRing, selectedDirection);
    }

    private static boolean isActiveAccelerationRing(BlockState state) {
        return state.hasProperty(AccelerationRingBlock.HALF)
               && state.getValue(AccelerationRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
               && state.getValue(AccelerationRingBlock.SWITCH) == IPowerComponent.Switch.ON
               && !state.getValue(AccelerationRingBlock.OVERLOAD);
    }

    public static boolean canBeAccelerated(Entity entity) {
        return entity instanceof FallingBlockEntity fallingBlockEntity
               && fallingBlockEntity.getBlockState().is(BlockTags.ANVIL)
               && !fallingBlockEntity.getBlockState().is(ModBlockTags.NON_MAGNETIC)
               || entity instanceof Projectile
               || (entity instanceof Player player && isPlayerCanBeAccelerated(player));
    }

    public static boolean isInsideAccelerationArea(Entity entity) {
        if (!canBeAccelerated(entity)) return false;
        Level level = entity.level();
        Vec3 center = getMovementCenter(entity);
        for (BlockPos pos : AccelerationRingBlockEntity.getAllBlocks(level)) {
            AABB aabb = AccelerationRingBlockEntity.getAABB(level, pos);
            if (aabb != null && aabb.contains(center) && isActiveAccelerationRing(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static AccelerationEntry findFirstAccelerationEntry(Entity entity, Vec3 movement) {
        if (!canBeAccelerated(entity)) return null;
        double movementSqr = movement.lengthSqr();
        if (!Double.isFinite(movementSqr)) return null;
        Level level = entity.level();
        Vec3 start = getMovementCenter(entity);
        Vec3 end = start.add(movement);
        AccelerationEntry nearestEntry = null;
        double nearestProgress = Double.POSITIVE_INFINITY;
        double bestAlignment = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : AccelerationRingBlockEntity.getAllBlocks(level)) {
            AABB aabb = AccelerationRingBlockEntity.getAABB(level, pos);
            if (aabb == null) continue;
            BlockState state = level.getBlockState(pos);
            if (!isActiveAccelerationRing(state)) continue;

            double progress;
            if (aabb.contains(start)) {
                progress = 0.0;
            } else {
                if (movementSqr < 1.0E-12) continue;
                var clipped = aabb.clip(start, end);
                if (clipped.isEmpty()) continue;
                progress = clipped.get().subtract(start).dot(movement) / movementSqr;
                if (progress < 0.0 || progress > 1.0) continue;
                double insideProgress = Math.min(1.0, progress + 1.0E-7);
                if (insideProgress > progress && !aabb.contains(start.add(movement.scale(insideProgress)))) continue;
            }

            Direction direction = state.getValue(AccelerationRingBlock.FACING);
            double alignment = movement.dot(Vec3.atLowerCornerOf(direction.getNormal()));
            if (progress > nearestProgress || progress == nearestProgress && alignment <= bestAlignment) continue;
            nearestEntry = new AccelerationEntry(pos, direction, progress);
            nearestProgress = progress;
            bestAlignment = alignment;
        }
        return nearestEntry;
    }

    public static boolean isControlledByRing(Entity entity) {
        if (!canBeAccelerated(entity)) return false;
        if (isInsideAccelerationArea(entity) || DeflectionRingBlockEntity.isInsideWorkingRing(entity)) return true;
        return DeflectionRingBlockEntity.findFirstRing(
            entity,
            getMovementCenter(entity),
            entity.getDeltaMovement()
        ) != null;
    }

    public static Vec3 getMovementCenter(Entity entity) {
        if (entity instanceof FallingBlockEntity) return entity.getBoundingBox().getCenter();
        if (entity instanceof Player) {
            return entity.position().add(0, 0.5, 0);
        }
        return entity.position();
    }

    public static Vec3 getMovementOffset(Entity entity) {
        return getMovementCenter(entity).subtract(entity.position());
    }

    public static Vec3 clampMovement(Entity entity, Vec3 movement) {
        double limit = entity instanceof Player ? MAX_PLAYER_SPEED : MAX_ACCELERATED_SPEED;
        double maxComponent = Math.max(Math.abs(movement.x), Math.max(Math.abs(movement.y), Math.abs(movement.z)));
        if (!Double.isFinite(maxComponent)) return Vec3.ZERO;
        if (maxComponent <= limit && movement.lengthSqr() <= limit * limit) return movement;
        if (maxComponent < 1.0E-12) return Vec3.ZERO;
        Vec3 scaled = movement.scale(1.0 / maxComponent);
        double scaledLength = scaled.length();
        if (!Double.isFinite(scaledLength) || scaledLength < 1.0E-12) return Vec3.ZERO;
        return scaled.scale(limit / scaledLength);
    }

    static boolean isPlayerCanBeAccelerated(Player player) {
        Iterable<ItemStack> armorSlots = player.getArmorSlots();
        boolean hasHammer = false;
        int count = 0;
        for (ItemStack stack : armorSlots) {
            if (stack.getItem() instanceof AnvilHammerItem) {
                hasHammer = true;
            }
            if (stack.getItem() instanceof ArmorItem) {
                count++;
            }
        }
        return count >= 2 && hasHammer;
    }

    public static void applyAcceleration(Entity entity, AccelerationEntry entry) {
        applyAcceleration(entity, entry.ringPos(), entry.direction());
    }

    private static void applyAcceleration(Entity entity, BlockPos ringPos, Direction direction) {
        Vec3 fixMovement = ringPos
            .getCenter()
            .subtract(getMovementCenter(entity));
        Vec3 deltaMovement = entity.getDeltaMovement();
        fixMovement = switch (direction.getAxis()) {
            case X -> fixMovement.multiply(0, 1, 1);
            case Y -> fixMovement.multiply(1, 0, 1);
            case Z -> fixMovement.multiply(1, 1, 0);
        };
        deltaMovement = switch (direction.getAxis()) {
            case X -> deltaMovement.multiply(1, 0, 0);
            case Y -> deltaMovement.multiply(0, 1, 0);
            case Z -> deltaMovement.multiply(0, 0, 1);
        };
        fixMovement = fixMovement.multiply(0.2, 0.2, 0.2);
        if (Math.abs(entity.getDeltaMovement().get(direction.getAxis())) <= 5) {
            deltaMovement = deltaMovement.add(fixMovement);
        } else {
            entity.setPos(entity.position().add(fixMovement.multiply(5, 5, 5)));
        }
        deltaMovement = deltaMovement.scale(1.0204081632653061)
            .add(new Vec3(0.1f, 0.1f, 0.1f).multiply(Vec3.atLowerCornerOf(direction.getNormal())));
        entity.setDeltaMovement(clampMovement(entity, deltaMovement));
    }

    public record AccelerationEntry(BlockPos ringPos, Direction direction, double progress) {
    }
}
