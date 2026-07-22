package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ShockContext(
    Level level, BlockPos centerPos, FallingGiantAnvilEntity fallingGiantAnvil, List<BlockPos> rangePosList, float fallDistance
) {

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
                if (!this.level.getBlockState(this.centerPos.relative(direction1).relative(direction2)).is(tagKey)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean testCorner(Holder<Block> block) {
        return this.testCorner(block.value());
    }

    public boolean testCorner(Block block) {
        for (Direction direction1 : HORIZONTAL_X) {
            for (Direction direction2 : HORIZONTAL_Z) {
                if (!this.level.getBlockState(this.centerPos.relative(direction1).relative(direction2)).is(block)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean testBorder(Holder<Block> block) {
        return this.testBorder(block.value());
    }

    public boolean testBorder(TagKey<Block> tagKey) {
        for (Direction direction : HORIZONTAL) {
            if (!this.level.getBlockState(this.centerPos.relative(direction)).is(tagKey)) {
                return false;
            }
        }
        return true;
    }

    public boolean testBorder(Block block) {
        for (Direction direction : HORIZONTAL) {
            if (!this.level.getBlockState(this.centerPos.relative(direction)).is(block)) {
                return false;
            }
        }
        return true;
    }

    public boolean testBorder(Class<? extends Block> block) {
        for (Direction direction : HORIZONTAL) {
            if (!block.isInstance(this.level.getBlockState(this.centerPos.relative(direction)).getBlock())) {
                return false;
            }
        }
        return true;
    }

    public Optional<BlockMiningEffect> getBorderMiningEffect() {
        BlockMiningEffect effect = null;
        for (Direction direction : HORIZONTAL) {
            Optional<BlockMiningEffect> current = BlockMiningEffect.fromAnvil(
                this.level.getBlockState(this.centerPos.relative(direction)).getBlock()
            );
            if (current.isEmpty()) return Optional.empty();
            if (effect == null) {
                effect = current.get();
            } else if (!effect.equals(current.get())) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(effect);
    }
}
