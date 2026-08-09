package dev.dubhe.anvilcraft.api.hammer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/// 可被锤子改变的方块
@SuppressWarnings("unused")
public interface HammerRotateBehavior extends IHammerChangeable {
    EnumProperty<Direction> FACING_HOPPER = BlockStateProperties.FACING_HOPPER;
    EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    HammerRotateBehavior DEFAULT = new HammerRotateBehavior() {
    };
    HammerRotateBehavior EMPTY = new HammerRotateBehavior() {
        public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
            return false;
        }
    };

    private static BlockState rotate(BlockState state) {
        Direction direction = state.getValue(HammerRotateBehavior.FACING);
        return switch (direction) {
            case WEST -> state.setValue(HammerRotateBehavior.FACING, Direction.UP);
            case UP -> state.setValue(HammerRotateBehavior.FACING, Direction.DOWN);
            case DOWN -> state.setValue(HammerRotateBehavior.FACING, Direction.NORTH);
            default -> state.setValue(HammerRotateBehavior.FACING, direction.getClockWise());
        };
    }

    private static BlockState hopperRotate(BlockState state) {
        Direction direction = state.getValue(HammerRotateBehavior.FACING_HOPPER);
        return switch (direction) {
            case WEST -> state.setValue(HammerRotateBehavior.FACING_HOPPER, Direction.DOWN);
            case DOWN -> state.setValue(HammerRotateBehavior.FACING_HOPPER, Direction.NORTH);
            default -> state.setValue(HammerRotateBehavior.FACING_HOPPER, direction.getClockWise());
        };
    }

    private static BlockState horizontalRotate(BlockState state) {
        return state.setValue(
            HammerRotateBehavior.HORIZONTAL_FACING,
            state.getValue(HammerRotateBehavior.HORIZONTAL_FACING).getClockWise()
        );
    }

    @Override
    default boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        if (state.hasProperty(HammerRotateBehavior.FACING)) {
            state = HammerRotateBehavior.rotate(state);
        } else {
            if (state.hasProperty(HammerRotateBehavior.FACING_HOPPER)) {
                state = HammerRotateBehavior.hopperRotate(state);
            } else {
                if (state.hasProperty(HammerRotateBehavior.HORIZONTAL_FACING)) {
                    state = HammerRotateBehavior.horizontalRotate(state);
                }
            }
        }
        level.setBlockAndUpdate(blockPos, state);
        return true;
    }

    @Override
    default @Nullable Property<?> getChangeableProperty(BlockState state) {
        if (state.hasProperty(HammerRotateBehavior.FACING)) {
            return HammerRotateBehavior.FACING;
        } else if (state.hasProperty(HammerRotateBehavior.FACING_HOPPER)) {
            return HammerRotateBehavior.FACING_HOPPER;
        } else if (state.hasProperty(HammerRotateBehavior.HORIZONTAL_FACING)) {
            return HammerRotateBehavior.HORIZONTAL_FACING;
        }
        return null;
    }
}
