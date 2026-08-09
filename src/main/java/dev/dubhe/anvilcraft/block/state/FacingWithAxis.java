package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public enum FacingWithAxis implements StringRepresentable {
    UP_X(Direction.UP, Direction.Axis.X),
    DOWN_X(Direction.DOWN, Direction.Axis.X),
    NORTH_X(Direction.NORTH, Direction.Axis.X),
    SOUTH_X(Direction.SOUTH, Direction.Axis.X),
    UP_Z(Direction.UP, Direction.Axis.Z),
    DOWN_Z(Direction.DOWN, Direction.Axis.Z),
    WEST_Z(Direction.WEST, Direction.Axis.Z),
    EAST_Z(Direction.EAST, Direction.Axis.Z),
    NORTH_Y(Direction.NORTH, Direction.Axis.Y),
    SOUTH_Y(Direction.SOUTH, Direction.Axis.Y),
    WEST_Y(Direction.WEST, Direction.Axis.Y),
    EAST_Y(Direction.EAST, Direction.Axis.Y);

    @Getter
    private final Direction facing;
    @Getter
    private final Direction.Axis axis;
    private final String name;

    FacingWithAxis(Direction facing, Direction.Axis axis) {
        this.facing = facing;
        this.axis = axis;
        this.name = facing.getSerializedName() + "_" + axis.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static FacingWithAxis of(Direction facing, Direction.Axis axis) {
        for (FacingWithAxis fwa : FacingWithAxis.values()) {
            if (fwa.facing == facing && fwa.axis == axis) {
                return fwa;
            }
        }
        return FacingWithAxis.NORTH_X;
    }

    public FacingWithAxis rotate(Rotation rotation) {
        Direction newFacing = rotation.rotate(this.facing);
        Direction.Axis newAxis = this.axis;
        if (this.facing.getAxis() == Direction.Axis.Y) {
            Direction axisDir = Direction.fromAxisAndDirection(this.axis, Direction.AxisDirection.POSITIVE);
            newAxis = rotation.rotate(axisDir).getAxis();
        }
        return FacingWithAxis.of(newFacing, newAxis);
    }

    public FacingWithAxis mirror(Mirror mirror) {
        return FacingWithAxis.of(mirror.mirror(this.facing), this.axis);
    }

    public FacingWithAxis toggleAxis() {
        return switch (this) {
            case NORTH_X -> FacingWithAxis.NORTH_Y;
            case NORTH_Y -> FacingWithAxis.NORTH_X;
            case SOUTH_X -> FacingWithAxis.SOUTH_Y;
            case SOUTH_Y -> FacingWithAxis.SOUTH_X;
            case EAST_Z -> FacingWithAxis.EAST_Y;
            case EAST_Y -> FacingWithAxis.EAST_Z;
            case WEST_Z -> FacingWithAxis.WEST_Y;
            case WEST_Y -> FacingWithAxis.WEST_Z;
            case UP_X -> FacingWithAxis.UP_Z;
            case UP_Z -> FacingWithAxis.UP_X;
            case DOWN_X -> FacingWithAxis.DOWN_Z;
            case DOWN_Z -> FacingWithAxis.DOWN_X;
        };
    }
}
