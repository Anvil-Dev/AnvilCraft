package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.function.UnaryOperator;

/// 红石元件的朝向保存在底座的局部坐标中，每个安装面对应四个面内方向。
public interface MountedRedstone extends IHammerChangeable {
    DirectionProperty ATTACHMENT = DirectionProperty.create("attachment");

    static BlockState placementState(BlockState base, BlockPlaceContext context, boolean facesPlayer) {
        Direction look = context.getHorizontalDirection();
        return base.setValue(ATTACHMENT, Direction.DOWN)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, facesPlayer ? look.getOpposite() : look);
    }

    static Map<Direction, VoxelShape> shapes(VoxelShape floor) {
        VoxelShape north = ShapeUtil.rotate(Direction.Axis.X, 270, floor);
        return Map.of(Direction.DOWN, floor, Direction.UP, ShapeUtil.rotate(Direction.Axis.X, 180, floor),
            Direction.NORTH, north, Direction.SOUTH, ShapeUtil.rotate(Direction.Axis.Y, 180, north),
            Direction.WEST, ShapeUtil.rotate(Direction.Axis.Y, 90, north), Direction.EAST, ShapeUtil.rotate(Direction.Axis.Y, 270, north));
    }

    static Direction inputDirection(BlockState state) {
        return worldDirection(state.getValue(ATTACHMENT), state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    static Direction worldDirection(Direction attachment, Direction facing) {
        if (attachment == Direction.DOWN) {
            return facing;
        }
        if (attachment == Direction.UP) {
            return facing.getAxis() == Direction.Axis.Z ? facing.getOpposite() : facing;
        }
        return switch (facing) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> attachment.getClockWise();
            case WEST -> attachment.getCounterClockWise();
            default -> throw new IllegalArgumentException(facing.getName());
        };
    }

    static BlockState transform(BlockState state, UnaryOperator<Direction> transform) {
        Direction attachment = transform.apply(state.getValue(ATTACHMENT));
        Direction input = transform.apply(inputDirection(state));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (worldDirection(attachment, facing) == input) {
                return state.setValue(ATTACHMENT, attachment).setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
        }
        throw new IllegalArgumentException(attachment + " / " + input);
    }

    @Override
    default boolean change(Player player, BlockPos pos, Level level, ItemStack hammer) {
        BlockState state = level.getBlockState(pos);
        return level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING,
            state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise()));
    }

    @Override
    default DirectionProperty getChangeableProperty(BlockState state) {
        return BlockStateProperties.HORIZONTAL_FACING;
    }
}
