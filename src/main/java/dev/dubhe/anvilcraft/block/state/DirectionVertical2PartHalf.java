package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.core.Direction;

@Getter
public enum DirectionVertical2PartHalf implements IFlexibleMultiPartBlockState<DirectionVertical2PartHalf, Direction> {
    TOP("top", 0, 1, 0),
    BOTTOM("bottom", 0, 0, 0);

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    DirectionVertical2PartHalf(String name, int offsetX, int offsetY, int offsetZ) {
        this.name = name;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public int getOffsetX(Direction value) {
        return this.getOffsetX();
    }

    @Override
    public int getOffsetY(Direction value) {
        return this.getOffsetY();
    }

    @Override
    public int getOffsetZ(Direction value) {
        return this.getOffsetZ();
    }

    @Override
    public boolean isMain() {
        return this == DirectionVertical2PartHalf.BOTTOM;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
