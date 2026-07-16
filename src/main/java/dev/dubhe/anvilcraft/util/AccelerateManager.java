package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.entity.RailgunAnvilEntity;
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
    private static final double WATERLOGGED_RING_SPEED = 1.0;

    public static void handleAcceleration(Entity entity) {
        if (!canBeAccelerated(entity)) return;
        Vec3 currentMovement = entity.getDeltaMovement();
        Vec3 clampedMovement = clampMovement(entity, currentMovement);
        if (clampedMovement != currentMovement) entity.setDeltaMovement(clampedMovement);
        Level level = entity.level();
        Vec3 center = getMovementCenter(entity);
        boolean passesWaterloggedRing = passesWaterloggedAccelerationRing(entity, center, clampedMovement);
        BlockPos selectedRing = null;
        Direction selectedDirection = null;
        double bestAlignment = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : AccelerationRingBlockEntity.getBlocksAt(level, center)) {
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
        if (passesWaterloggedRing) {
            entity.setDeltaMovement(limitAnvilSpeed(entity, entity.getDeltaMovement()));
        }
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
        for (BlockPos pos : AccelerationRingBlockEntity.getBlocksAt(level, center)) {
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
        for (BlockPos pos : AccelerationRingBlockEntity.getBlocksAlongMovement(level, start, movement)) {
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

    public static Vec3 limitAnvilSpeed(Entity entity, Vec3 movement) {
        if (!isAnvil(entity)) return movement;
        double speedSqr = movement.lengthSqr();
        if (!Double.isFinite(speedSqr)) return Vec3.ZERO;
        double limitSqr = WATERLOGGED_RING_SPEED * WATERLOGGED_RING_SPEED;
        if (speedSqr <= limitSqr) return movement;
        return movement.scale(WATERLOGGED_RING_SPEED / Math.sqrt(speedSqr));
    }

    private static boolean isAnvil(Entity entity) {
        if (entity instanceof FallingBlockEntity falling) return falling.getBlockState().is(BlockTags.ANVIL);
        return entity instanceof RailgunAnvilEntity railgun && railgun.getBlockState().is(BlockTags.ANVIL);
    }

    private static boolean passesWaterloggedAccelerationRing(Entity entity, Vec3 start, Vec3 movement) {
        if (!isAnvil(entity)) return false;
        double movementSqr = movement.lengthSqr();
        if (!Double.isFinite(movementSqr) || movementSqr < 1.0E-12) return false;
        double waterloggedProgress = firstWaterloggedAccelerationRingProgress(
            entity.level(),
            start,
            movement,
            movementSqr
        );
        if (!Double.isFinite(waterloggedProgress)) return false;
        BlockPos deflectionRing = DeflectionRingBlockEntity.findFirstRing(entity, start, movement);
        if (deflectionRing == null) return true;
        double deflectionProgress = deflectionRing.getCenter().subtract(start).dot(movement) / movementSqr;
        return waterloggedProgress <= deflectionProgress;
    }

    private static double firstWaterloggedAccelerationRingProgress(
        Level level,
        Vec3 start,
        Vec3 movement,
        double movementSqr
    ) {
        Vec3 end = start.add(movement);
        double nearestProgress = Double.POSITIVE_INFINITY;
        for (BlockPos ringPos : AccelerationRingBlockEntity.getRingsAlongMovement(level, start, movement)) {
            BlockState state = level.getBlockState(ringPos);
            if (!(state.getBlock() instanceof AccelerationRingBlock block)
                || !isActiveAccelerationRing(state)
                || !block.isChannelWaterlogged(level, ringPos, state)) {
                continue;
            }
            Direction positive = switch (state.getValue(AccelerationRingBlock.FACING).getAxis()) {
                case X -> Direction.EAST;
                case Y -> Direction.UP;
                case Z -> Direction.SOUTH;
            };
            AABB channel = AABB.encapsulatingFullBlocks(
                ringPos.relative(positive.getOpposite()),
                ringPos.relative(positive)
            );
            double progress;
            if (channel.contains(start)) {
                progress = 0.0;
            } else {
                var clipped = channel.clip(start, end);
                if (clipped.isEmpty()) continue;
                progress = clipped.get().subtract(start).dot(movement) / movementSqr;
            }
            if (progress >= 0.0 && progress <= 1.0 && progress < nearestProgress) {
                nearestProgress = progress;
            }
        }
        return nearestProgress;
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
        BlockState state = entity.level().getBlockState(entry.ringPos());
        if (state.getBlock() instanceof AccelerationRingBlock block
            && block.isChannelWaterlogged(entity.level(), entry.ringPos(), state)) {
            entity.setDeltaMovement(limitAnvilSpeed(entity, entity.getDeltaMovement()));
        }
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
