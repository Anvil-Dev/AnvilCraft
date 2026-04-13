package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
public enum OpenedCube3x3PartHalf implements IFlexibleMultiPartBlockState<OpenedCube3x3PartHalf, Boolean>,
    ISimpleMultiPartBlockState<OpenedCube3x3PartHalf> {
    BOTTOM_CENTER("bottom_center", 0, 0, 0),
    BOTTOM_W("bottom_w", -1, 0, 0),
    BOTTOM_E("bottom_e", 1, 0, 0),
    BOTTOM_N("bottom_n", 0, 0, -1),
    BOTTOM_S("bottom_s", 0, 0, 1),
    BOTTOM_NW("bottom_wn", -1, 0, -1),
    BOTTOM_SW("bottom_ws", -1, 0, 1),
    BOTTOM_NE("bottom_en", 1, 0, -1),
    BOTTOM_SE("bottom_es", 1, 0, 1),
    MID_CENTER("mid_center", 0, 1, 0),
    MID_W("mid_w", -1, 1, 0),
    MID_E("mid_e", 1, 1, 0),
    MID_N("mid_n", 0, 1, -1),
    MID_S("mid_s", 0, 1, 1),
    MID_NW("mid_wn", -1, 1, -1),
    MID_SW("mid_ws", -1, 1, 1),
    MID_NE("mid_en", 1, 1, -1),
    MID_SE("mid_es", 1, 1, 1),
    TOP_CENTER("top_center", 0, 2, 0),
    TOP_W("top_w", -1, 2, 0),
    TOP_E("top_e", 1, 2, 0),
    TOP_N("top_n", 0, 2, -1),
    TOP_S("top_s", 0, 2, 1),
    TOP_NW("top_wn", -1, 2, -1),
    TOP_SW("top_ws", -1, 2, 1),
    TOP_NE("top_en", 1, 2, -1),
    TOP_SE("top_es", 1, 2, 1);

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private OpenedCube3x3PartHalf clockwise90;
    private OpenedCube3x3PartHalf clockwise180;
    private OpenedCube3x3PartHalf clockwise270;
    private OpenedCube3x3PartHalf mirrorX;
    private OpenedCube3x3PartHalf mirrorZ;

    @Nullable
    public static OpenedCube3x3PartHalf findByOffset(int offsetX, int offsetY, int offsetZ) {
        return Arrays.stream(OpenedCube3x3PartHalf.values())
            .filter(part -> part.offsetX == offsetX)
            .filter(part -> part.offsetY == offsetY)
            .filter(part -> part.offsetZ == offsetZ)
            .findFirst()
            .orElse(null);
    }

    static {
        for (OpenedCube3x3PartHalf half : OpenedCube3x3PartHalf.values()) {
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

    OpenedCube3x3PartHalf(String name, int offsetX, int offsetY, int offsetZ) {
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

    public OpenedCube3x3PartHalf rotate(Rotation rotation) {
        return switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> this.clockwise90;
            case CLOCKWISE_180 -> this.clockwise180;
            case COUNTERCLOCKWISE_90 -> this.clockwise270;
        };
    }

    public OpenedCube3x3PartHalf mirror(Mirror mirror) {
        return switch (mirror) {
            case NONE -> this;
            case LEFT_RIGHT -> this.mirrorZ;
            case FRONT_BACK -> this.mirrorX;
        };
    }

    @Override
    public int getOffsetX(Boolean value) {
        return this.offsetX;
    }

    @Override
    public int getOffsetY(Boolean value) {
        return this.offsetY;
    }

    @Override
    public int getOffsetZ(Boolean value) {
        return this.offsetZ;
    }

    @Override
    public boolean isMain() {
        return this == OpenedCube3x3PartHalf.MID_CENTER;
    }

    public BlockPos fromMain(BlockPos pos) {
        return pos.offset(this.offsetX, this.offsetY, this.offsetZ).below();
    }

    public BlockPos toMain(BlockPos pos) {
        return pos.offset(-this.offsetX, -this.offsetY, -this.offsetZ).above();
    }
}