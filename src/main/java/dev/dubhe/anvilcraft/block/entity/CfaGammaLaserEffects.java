package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.RubyPrismBlock;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.heatable.OverheatedEmberMetalBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Shared world effects for the CFA gamma beam emitters. */
final class CfaGammaLaserEffects {
    private static final int MAX_DISTANCE = 16;

    private CfaGammaLaserEffects() {
    }

    static BlockPos findTarget(Level level, BlockPos origin, Direction direction) {
        for (int distance = 1; distance <= MAX_DISTANCE; distance++) {
            BlockPos candidate = origin.relative(direction, distance);
            if (!level.getBlockState(candidate).is(BlockTags.REPLACEABLE)) return candidate;
        }
        return origin.relative(direction, MAX_DISTANCE);
    }

    static void destroyPrisms(Level level, BlockPos origin, Direction direction, BlockPos target) {
        BlockPos.MutableBlockPos current = origin.relative(direction).mutable();
        while (!current.equals(target)) {
            if (level.getBlockState(current).getBlock() instanceof RubyPrismBlock) {
                level.destroyBlock(current.immutable(), true);
            }
            current.move(direction);
        }
    }

    static void damageEntities(Level level, BlockPos origin, BlockPos target, Direction direction, int gammaLevel) {
        int damage = Math.min(16, gammaLevel - 4) * 16;
        if (damage <= 0) return;
        Vec3 start = origin.relative(direction).getCenter().add(-0.0625, -0.0625, -0.0625);
        Vec3 end = target.relative(direction.getOpposite()).getCenter().add(0.0625, 0.0625, 0.0625);
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new AABB(start, end), Entity::isAlive)
            .forEach(entity -> entity.hurt(ModDamageTypes.gammaLaser(level), damage));
    }

    static void heatEmberMetal(
        Level level,
        @Nullable BlockPos target,
        Direction direction,
        int gammaLevel,
        int updateFlags
    ) {
        if (target == null || gammaLevel < 4 || level.getGameTime() % 20 != 0) return;
        BlockState hitState = level.getBlockState(target);
        if (!hitState.is(ModBlocks.EMBER_METAL_BLOCK.get())
            && !hitState.is(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())) {
            return;
        }
        int areaSize = gammaLevel >= 16 ? 7 : gammaLevel >= 12 ? 5 : gammaLevel >= 8 ? 3 : 1;
        int thickness = gammaLevel >= 16 ? 3 : gammaLevel >= 12 ? 2 : 1;
        int halfSize = areaSize / 2;
        Direction[] perpendiculars = switch (direction.getAxis()) {
            case X -> new Direction[]{Direction.UP, Direction.NORTH};
            case Z -> new Direction[]{Direction.UP, Direction.EAST};
            default -> new Direction[]{Direction.NORTH, Direction.EAST};
        };

        for (int depth = 0; depth < thickness; depth++) {
            BlockPos depthPos = target.relative(direction, depth);
            for (int first = -halfSize; first <= halfSize; first++) {
                for (int second = -halfSize; second <= halfSize; second++) {
                    heatEmberMetalAt(level, depthPos
                        .relative(perpendiculars[0], first)
                        .relative(perpendiculars[1], second), updateFlags);
                }
            }
        }
    }

    private static void heatEmberMetalAt(Level level, BlockPos pos, int updateFlags) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.EMBER_METAL_BLOCK.get())) {
            OverheatedEmberMetalBlock overheated = ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get();
            BlockState overheatedState = overheated.defaultBlockState();
            level.setBlock(pos, overheatedState, updateFlags);
            BlockEntity blockEntity = overheated.newBlockEntity(pos, overheatedState);
            if (blockEntity instanceof HeatableBlockEntity heatable) {
                level.setBlockEntity(heatable);
                heatable.addDurationInTick(80);
            }
        } else if (state.is(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())
            && level.getBlockEntity(pos) instanceof HeatableBlockEntity heatable) {
            heatable.addDurationInTick(80);
        }
    }
}
