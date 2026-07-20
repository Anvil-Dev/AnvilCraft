package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.giantanvil.IShockEntity;
import dev.dubhe.anvilcraft.api.giantanvil.ShockAnvilBehavior;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ShockContext(
    Level level, BlockPos centerPos, FallingGiantAnvilEntity fallingGiantAnvil, List<BlockPos> rangePosList, float fallDistance
) {
    /** 本体方块铁砧在弹性模式下使用的初始竖直速度。 */
    public static final double DEFAULT_BOUNCE_VELOCITY = 0.31D;

    public static final Direction[] HORIZONTAL = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST
    };

    public static final Direction[] HORIZONTAL_X = {
        Direction.EAST,
        Direction.WEST
    };

    public static final Direction[] HORIZONTAL_Z = {
        Direction.SOUTH,
        Direction.NORTH
    };

    public static ShockContext inflate(AnvilEvent.GiantOnLand event) {
        BlockPos detectCenter = event.getPos().below(2);
        BlockPos ground = detectCenter.above();
        List<BlockPos> rangePosList = new ArrayList<>();
        int radius = (int) Math.min(Math.ceil(event.getFallDistance()), AnvilCraft.CONFIG.giantAnvilMaxShockRadius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = ground.offset(dx, 0, dz);
                rangePosList.add(pos);
            }
        }
        return new ShockContext(event.getLevel(), detectCenter, event.getEntity(), rangePosList, event.getFallDistance());
    }

    public boolean testCorner(TagKey<Block> tagKey) {
        for (Direction direction1 : HORIZONTAL_X) {
            for (Direction direction2 : HORIZONTAL_Z) {
                if (!matchesShockBase(centerPos.relative(direction1).relative(direction2), tagKey)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean testCorner(Holder<Block> block) {
        return testCorner(block.value());
    }

    public boolean testCorner(Block block) {
        for (Direction direction1 : HORIZONTAL_X) {
            for (Direction direction2 : HORIZONTAL_Z) {
                if (!matchesShockBase(centerPos.relative(direction1).relative(direction2), block)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean testBorder(Holder<Block> block) {
        return testBorder(block.value());
    }

    public boolean testBorder(TagKey<Block> tagKey) {
        for (Direction direction : HORIZONTAL) {
            if (!matchesShockBase(centerPos.relative(direction), tagKey)) {
                return false;
            }
        }
        return true;
    }

    public boolean testBorder(Block block) {
        for (Direction direction : HORIZONTAL) {
            if (!matchesShockBase(centerPos.relative(direction), block)) {
                return false;
            }
        }
        return true;
    }

    public boolean testBorder(Class<? extends Block> block) {
        for (Direction direction : HORIZONTAL) {
            BlockPos pos = centerPos.relative(direction);
            if (block.isInstance(level.getBlockState(pos).getBlock())) continue;
            boolean entityMatches = shockEntitiesAt(pos).stream()
                .map(IShockEntity::anvilcraft$getShockBaseState)
                .flatMap(Optional::stream)
                .map(BlockBehaviour.BlockStateBase::getBlock)
                .anyMatch(block::isInstance);
            if (!entityMatches) return false;
        }
        return true;
    }

    /** 保留旧的挖掘效果查询接口，并将实体铁砧纳入查询。 */
    public Optional<BlockMiningEffect> getBorderMiningEffect() {
        return getBorderAnvilBehavior().map(ShockAnvilBehavior::miningEffect);
    }

    /** 返回四个边框铁砧共同提供的完整撼地行为。 */
    public Optional<ShockAnvilBehavior> getBorderAnvilBehavior() {
        ShockAnvilBehavior behavior = null;
        for (Direction direction : HORIZONTAL) {
            Optional<ShockAnvilBehavior> current = getAnvilBehaviorAt(centerPos.relative(direction));
            if (current.isEmpty()) return Optional.empty();
            if (behavior == null) {
                behavior = current.get();
            } else if (!behavior.isCompatibleWith(current.get())) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(behavior);
    }

    /** 返回撼地范围内声明参与弹性模式的实体。 */
    public List<Entity> getShockEntitiesInRange() {
        if (rangePosList.isEmpty()) return List.of();
        int radius = getShockRadius();
        BlockPos ground = centerPos.above();
        AABB range = new AABB(
            ground.getX() - radius,
            ground.getY(),
            ground.getZ() - radius,
            ground.getX() + radius + 1.0D,
            ground.getY() + 1.0D,
            ground.getZ() + radius + 1.0D
        );
        return level.getEntitiesOfClass(
            Entity.class,
            range,
            entity -> entity instanceof IShockEntity && !entity.isRemoved()
        );
    }

    /** 返回撼地范围的方形半径。 */
    public int getShockRadius() {
        int radius = 0;
        for (BlockPos pos : rangePosList) {
            radius = Math.max(
                radius,
                Math.max(Math.abs(pos.getX() - centerPos.getX()), Math.abs(pos.getZ() - centerPos.getZ()))
            );
        }
        return radius;
    }

    /** 按目标高度倍率计算与本体方块铁砧一致重力下的弹起速度。 */
    public static double bounceVelocityForHeight(double heightMultiplier) {
        if (!Double.isFinite(heightMultiplier) || heightMultiplier <= 0.0D) return 0.0D;
        return DEFAULT_BOUNCE_VELOCITY * Math.sqrt(heightMultiplier);
    }

    private Optional<ShockAnvilBehavior> getAnvilBehaviorAt(BlockPos pos) {
        for (IShockEntity entity : shockEntitiesAt(pos)) {
            Optional<ShockAnvilBehavior> behavior = entity.anvilcraft$getShockAnvilBehavior();
            if (behavior.isPresent()) return behavior;
        }
        return BlockMiningEffect.fromAnvil(level.getBlockState(pos).getBlock())
            .map(ShockAnvilBehavior::fromMiningEffect);
    }

    private boolean matchesShockBase(BlockPos pos, TagKey<Block> tagKey) {
        if (level.getBlockState(pos).is(tagKey)) return true;
        return shockEntitiesAt(pos).stream()
            .map(IShockEntity::anvilcraft$getShockBaseState)
            .flatMap(Optional::stream)
            .anyMatch(state -> state.is(tagKey));
    }

    private boolean matchesShockBase(BlockPos pos, Block block) {
        if (level.getBlockState(pos).is(block)) return true;
        return shockEntitiesAt(pos).stream()
            .map(IShockEntity::anvilcraft$getShockBaseState)
            .flatMap(Optional::stream)
            .anyMatch(state -> state.is(block));
    }

    private List<IShockEntity> shockEntitiesAt(BlockPos pos) {
        return level.getEntitiesOfClass(
                Entity.class,
                new AABB(pos),
                entity -> entity instanceof IShockEntity && !entity.isRemoved()
            ).stream()
            .map(entity -> (IShockEntity) entity)
            .toList();
    }
}
