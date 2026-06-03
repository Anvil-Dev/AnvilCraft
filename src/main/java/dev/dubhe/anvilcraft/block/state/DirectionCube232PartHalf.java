package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
public enum DirectionCube232PartHalf
    implements IFlexibleMultiPartBlockState<DirectionCube232PartHalf, Direction>, ISimpleMultiPartBlockState<DirectionCube232PartHalf> {
    BOTTOM_PART("bottom_part", 0, 0, 0),
    BOTTOM_W("bottom_w", -1, 0, 0),
    BOTTOM_S("bottom_s", 0, 0, -1),
    BOTTOM_WS("bottom_ws", -1, 0, -1),
    MID_PART("mid_part", 0, 1, 0),
    MID_W("mid_w", -1, 1, 0),
    MID_S("mid_s", 0, 1, -1),
    MID_WS("mid_ws", -1, 1, -1),
    TOP_PART("top_part", 0, 2, 0),
    TOP_W("top_w", -1, 2, 0),
    TOP_S("top_s", 0, 2, -1),
    TOP_WS("top_ws", -1, 2, -1);

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private DirectionCube232PartHalf clockwise90;
    private DirectionCube232PartHalf clockwise180;
    private DirectionCube232PartHalf clockwise270;
    private DirectionCube232PartHalf mirrorX;
    private DirectionCube232PartHalf mirrorZ;

    @Nullable
    public static DirectionCube232PartHalf findByOffset(int offsetX, int offsetY, int offsetZ) {
        return Arrays.stream(DirectionCube232PartHalf.values())
            .filter(part -> part.offsetX == offsetX)
            .filter(part -> part.offsetY == offsetY)
            .filter(part -> part.offsetZ == offsetZ)
            .findFirst()
            .orElse(null);
    }

    static {
        for (DirectionCube232PartHalf half : DirectionCube232PartHalf.values()) {
            int x = half.offsetX;
            int y = half.offsetY;
            int z = half.offsetZ;
            half.clockwise90 = findByOffset(-z, y, x);
            half.clockwise180 = findByOffset(-x, y, -z);
            half.clockwise270 = findByOffset(z, y, -x);
            half.mirrorX = findByOffset(-x, y, z);
            half.mirrorZ = findByOffset(x, y, -z);
        }
    }

    DirectionCube232PartHalf(String name, int offsetX, int offsetY, int offsetZ) {
        this.name = name;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public int getOffsetX(Direction value) {
        return this.offsetX;
    }

    @Override
    public int getOffsetY(Direction value) {
        return this.offsetY;
    }

    @Override
    public int getOffsetZ(Direction value) {
        return this.offsetZ;
    }

    @Override
    public boolean isMain() {
        return this == BOTTOM_PART;
    }

    public DirectionCube232PartHalf rotate(Rotation rotation) {
        return switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> this.clockwise90 != null ? this.clockwise90 : this;
            case CLOCKWISE_180 -> this.clockwise180 != null ? this.clockwise180 : this;
            case COUNTERCLOCKWISE_90 -> this.clockwise270 != null ? this.clockwise270 : this;
        };
    }

    public DirectionCube232PartHalf mirror(Mirror mirror) {
        return switch (mirror) {
            case NONE -> this;
            case LEFT_RIGHT -> this.mirrorZ != null ? this.mirrorZ : this;
            case FRONT_BACK -> this.mirrorX != null ? this.mirrorX : this;
        };
    }
}
